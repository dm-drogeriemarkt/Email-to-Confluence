package it.de.dm.mail2blog;

import com.atlassian.confluence.pages.BlogPost;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.user.User;
import com.atlassian.user.impl.DefaultUser;
import com.atlassian.user.security.password.Credential;
import de.dm.mail2blog.*;
import de.saly.javamail.mock2.MailboxFolder;
import de.saly.javamail.mock2.MockMailbox;
import lombok.extern.slf4j.Slf4j;

import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.HashMap;
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
    HashMap<String, Space> spaces = new HashMap<String, Space>();;
    MockMailbox mockMailbox;

    private static final int MESSAGE_COUNT = 2;
    private static final String SPACE_KEY_A = "testMail2blog";
    private static final String SPACE_KEY_B = "testMail2blog2";
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
        .defaultSpace(SPACE_KEY_A)
        .defaultContentType(ContentTypes.BlogPost)
        .spaceRules(new SpaceRule[]{
            SpaceRule.builder()
            .field(SpaceRuleFields.SUBJECT)
            .operator(SpaceRuleOperators.Is)
            .value("Hello")
            .action(SpaceRuleActions.MOVE)
            .space(SPACE_KEY_B)
            .contentType(ContentTypes.Page)
            .build()
        })
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
     * Create the test spaces.
     */
    private void setUpSpaces() throws Exception {
        tearDownSpaces();
        for (String key : (new String[]{SPACE_KEY_A, SPACE_KEY_B})) {
            if (!spaces.containsKey(key)) {
                Space space = StaticAccessor.getSpaceManager().createSpace(key, key, "Mail2Blog Test Space", user);
                StaticAccessor.getSpaceManager().saveSpace(space);
                spaces.put(key, space);
            }
        }
    }

    /**
     * Remove the test spaces.
     */
    private void tearDownSpaces() {
        for (String key : (new String[]{SPACE_KEY_A, SPACE_KEY_B})) {
            if (spaces.containsKey(key)) {spaces.remove(key);}
            Space space = StaticAccessor.getSpaceManager().getSpace(key);
            if (space != null) {
                StaticAccessor.getSpaceManager().removeSpace(space);
            }
        }
    }

    public void validatePosts() throws Exception {
        assertNotNull(spaces.get(SPACE_KEY_A));
        assertNotNull(spaces.get(SPACE_KEY_B));

        List<BlogPost> blogPosts = StaticAccessor.getPageManager().getBlogPosts(spaces.get(SPACE_KEY_A), false);
        assertEquals("Expected 1 blog posts", 1, blogPosts.size());

        BlogPost post1 = blogPosts.get(0);
        assertEquals("Wrong title on blog post", "Test", post1.getTitle());
        assertTrue("Wrong content in blog post", post1.getBodyAsString().contains("<p>Lieber Bob,</p>"));

        List<Page> pages = StaticAccessor.getPageManager().getPages(spaces.get(SPACE_KEY_B), false);
        assertEquals("Expected 2 pages", 2, pages.size());

        Page page1 = pages.get(1);
        assertEquals("Wrong title on page", "Hello", page1.getTitle());
        assertTrue("Wrong content in page", page1.getBodyAsString().contains("Hello World"));
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
        setUpSpaces();
    }

    public void testProcess() throws Exception
    {
        // Save the configuration
        StaticAccessor.getMailConfigurationManager().saveConfig(mailConfiguration);

        // Reset global state, so that config gets fetched from disk.
        StaticAccessor.getGlobalState().setMailConfigurationWrapper(null);

        // Run job
        StaticAccessor.getMail2BlogJob().runJob(null);

        validatePosts();
        validateMailbox();
    }
}
