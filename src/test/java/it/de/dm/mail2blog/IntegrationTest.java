package it.de.dm.mail2blog;

import com.atlassian.confluence.pages.BlogPost;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.user.User;
import com.atlassian.user.impl.DefaultUser;
import com.atlassian.user.security.password.Credential;
import de.dm.mail2blog.MailConfiguration;
import de.dm.mail2blog.StaticAccessor;
import de.saly.javamail.mock2.MailboxFolder;
import de.saly.javamail.mock2.MockMailbox;
import lombok.extern.slf4j.Slf4j;

import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Create a mailbox with messages and trigger the Mail2Blog job.
 * Then look if all messages have been successfully processed.
 *
 * The wired test framework just failed to work too many times so instead use the rest api for testing.
 */
@Slf4j
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
        user = StaticAccessor.getUserAccessor().getUser(USERNAME);
        tearDownUser();
        if (user == null) {
            user = StaticAccessor.getUserAccessor().createUser(
                    new DefaultUser(USERNAME, USERNAME, EMAIL),
                    Credential.unencrypted("password")
            );
            StaticAccessor.getUserAccessor().saveUser(user);
        }
    }

    /**
     * Remove test user.
     */
    private void tearDownUser() throws Exception {
        if (user != null && StaticAccessor.getUserAccessor().isUserRemovable(user)) {
            StaticAccessor.getUserAccessor().removeUser(user);
            user = null;
        }
    }

    /**
     * Create the test space.
     */
    private void setUpSpace() throws Exception {
        space = StaticAccessor.getSpaceManager().getSpace(SPACE_KEY);
        tearDownSpace();
        if (space == null) {
            space = StaticAccessor.getSpaceManager().createSpace(SPACE_KEY, SPACE_KEY, "Mail2Blog Test Space", user);
            StaticAccessor.getSpaceManager().saveSpace(space);
        }
    }

    /**
     * Remove the test space.
     */
    private void tearDownSpace() {
        if (space != null) {
            List test = space.getPageTemplates();
            StaticAccessor.getSpaceManager().removeSpace(space);
            space = null;
        }
    }

    public void validateBlogPosts() throws Exception {
        List<BlogPost> blogPosts = StaticAccessor.getPageManager().getBlogPosts(space, false);
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

    public void setUp() throws Exception {
        setUpMailbox();
        setUpConfiguration();
        setUpUser();
        setUpSpace();
    }

    public void testProcess() throws Exception
    {
        try {
            // Save the configuration
            StaticAccessor.getMailConfigurationManager().saveConfig(mailConfiguration);

            // Reset global state, so that config gets fetched from disk.
            StaticAccessor.getGlobalState().setMailConfigurationWrapper(null);

            // Run job
            StaticAccessor.getMail2BlogJob().runJob(null);

            validateBlogPosts();
            validateMailbox();
        } catch (Exception e) {
            fail("Exception in testProcess(): " + e.getMessage());
        }
    }
}
