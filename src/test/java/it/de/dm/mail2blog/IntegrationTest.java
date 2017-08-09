package it.de.dm.mail2blog;

import com.atlassian.confluence.pages.BlogPost;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;
import com.atlassian.user.User;
import com.atlassian.user.impl.DefaultUser;
import com.atlassian.user.security.password.Credential;
import de.dm.mail2blog.IMail2BlogJob;
import de.dm.mail2blog.IMailConfigurationManager;
import de.dm.mail2blog.MailConfiguration;
import de.saly.javamail.mock2.MailboxFolder;
import de.saly.javamail.mock2.MockMailbox;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Create a mailbox with messages and trigger the Mail2Blog job.
 * Then look if all messages have been successfully processed.
 * Uses the wired test interface by atlassian which runs a new confluence instance in the background.
 */
@RunWith(AtlassianPluginsTestRunner.class)
@RequiredArgsConstructor
public class IntegrationTest
{
    MailConfiguration mailConfiguration;
    User user;
    Space space;
    MockMailbox mockMailbox;

    private static final int MESSAGE_COUNT = 2;
    private static final String SPACE_KEY = "testMail2blog";
    private static final String USERNAME = "testMail2blog";
    private static final String EMAIL = "bob@example.org";

    // Auto wired components.
    @NonNull @Setter private IMailConfigurationManager mailConfigurationManager;
    @NonNull @Setter private SpaceManager spaceManager;
    @NonNull @Setter private UserAccessor userAccessor;
    @NonNull @Setter private IMail2BlogJob mail2BlogJob;
    @NonNull @Setter private PageManager pageManager;

    /**
     * Generate the mail configuration.
     */
    private void setUpConfiguration() throws Exception {
        mailConfiguration = MailConfiguration.builder()
        .server("mail.example.org")
        .secure(false)
        .username(EMAIL)
        .password("password")
        .protocol("mock_imap")
        .emailaddress(EMAIL)
        .defaultSpace(SPACE_KEY)
        .build();
    }

    /**
     * Create a mail server with messages.
     */
    private void setUpMailbox() throws Exception {
        tearDownMailbox();

        mockMailbox = MockMailbox.get(EMAIL);
        MailboxFolder remoteInbox = mockMailbox.getInbox();

        // Load all messages from resources/mailbox into the INBOX.
        InputStream is1 = IntegrationTest.class.getClassLoader().getResourceAsStream("mailbox/Hello.eml");
        MimeMessage message1 = new MimeMessage(null, is1);
        remoteInbox.add(message1);

        InputStream is2 = IntegrationTest.class.getClassLoader().getResourceAsStream("mailbox/Test.eml");
        MimeMessage message2 = new MimeMessage(null, is2);
        remoteInbox.add(message2);
    }

    /**
     * Destroy test mailbox.
     */
    private void tearDownMailbox() throws Exception {
        MockMailbox.resetAll();
    }

    /**
     * Create a test user.
     */
    private void setUpUser() throws Exception {
        user = userAccessor.getUserByName(USERNAME);
        tearDownUser();
        if (user == null) {
            user = userAccessor.createUser(
                    new DefaultUser(USERNAME, USERNAME, EMAIL),
                    Credential.unencrypted("password")
            );
            userAccessor.saveUser(user);
        }
    }

    /**
     * Remove test user.
     */
    private void tearDownUser() throws Exception {
        if (user != null && userAccessor.isUserRemovable(user)) {
            userAccessor.removeUser(user);
            user = null;
        }
    }

    /**
     * Create the test space.
     */
    private void setUpSpace() throws Exception {
        space = spaceManager.getSpace(SPACE_KEY);
        tearDownSpace();
        if (space == null) {
            space = spaceManager.createSpace(SPACE_KEY, SPACE_KEY, "Mail2Blog Test Space", user);
            spaceManager.saveSpace(space);
        }
    }

    /**
     * Remove the test space.
     */
    private void tearDownSpace() {
        if (space != null) {
            List test = space.getPageTemplates();
            spaceManager.removeSpace(space);
            space = null;
        }
    }

    public void validateBlogPosts() throws Exception {
        List<BlogPost> blogPosts = pageManager.getBlogPosts(space, false);
        assertEquals("Expected 2 blog posts", 2, blogPosts.size());

        BlogPost post1 = blogPosts.get(0);
        assertEquals("Wrong title on first blog post", "Test", post1.getTitle());
        assertTrue("Wrong content in first blog post", post1.getBodyAsString().contains("<p>Lieber Bob,</p>"));

        BlogPost post2 = blogPosts.get(1);
        assertEquals("Wrong title on second blog post", "Hello", post2.getTitle());
        assertTrue("Wrong content in second blog post", post2.getBodyAsString().contains("Hello World"));
    }

    public void validateMailbox() throws Exception {
        assertEquals("Expected an empty mailbox.", 0, mockMailbox.getInbox().getMessageCount());

        MailboxFolder processed = mockMailbox.getInbox().getOrAddSubFolder("Processed");
        assertEquals("Not all messages ended up in the processed folder.", MESSAGE_COUNT, processed.getMessageCount());
    }

    @Before
    public void setUp() throws Exception {
        try {
            setUpMailbox();
            setUpConfiguration();
            setUpUser();
            setUpSpace();
        } catch (Exception e) {
            fail("Exception in setUp(): " + e.getMessage());
        }
    }

    @Test
    public void testProcess() throws Exception
    {
        try {
            // Save the configuration
            mailConfigurationManager.saveConfig(mailConfiguration);

            // Run job
            mail2BlogJob.runJob(null);

            validateBlogPosts();
            validateMailbox();
        } catch (Exception e) {
            fail("Exception in testProcess(): " + e.getMessage());
        }
    }
}
