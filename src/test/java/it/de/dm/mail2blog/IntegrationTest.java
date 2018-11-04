package it.de.dm.mail2blog;

import com.atlassian.confluence.pages.BlogPost;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.user.User;
import com.atlassian.user.impl.DefaultUser;
import com.atlassian.user.security.password.Credential;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.sun.mail.imap.IMAPStore;
import de.dm.mail2blog.MailConfiguration;
import de.dm.mail2blog.StaticAccessor;
import de.dm.mail2blog.base.*;
import lombok.extern.slf4j.Slf4j;

import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.icegreen.greenmail.util.ServerSetupTest.IMAPS;
import static com.icegreen.greenmail.util.ServerSetupTest.POP3;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    GreenMail greenMail;

    private static final int MESSAGE_COUNT = 2;
    private static final String SPACE_KEY_A = "testMail2blog";
    private static final String SPACE_KEY_B = "testMail2blog2";
    private static final String USERNAME = "testMail2blog";
    private static final String EMAIL = "bob@example.org";
    private static final String PASSWORD = "password";

    /**
     * Generate the mail configuration.
     */
    private void setUpConfiguration() throws Exception {
        mailConfiguration = MailConfiguration.builder()
        .server("127.0.0.1")
        .secure(false)
        .username(USERNAME)
        .password(PASSWORD)
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
        greenMail = new GreenMail(new ServerSetup[]{POP3, IMAPS});
        greenMail.start();

        GreenMailUser user = greenMail.setUser(EMAIL, USERNAME, PASSWORD);

        // Load all messages from resources/mailbox into the INBOX.
        InputStream is1 = IntegrationTest.class.getClassLoader().getResourceAsStream("mailbox/Hello.eml");
        MimeMessage message1 = new MimeMessage(null, is1);
        user.deliver(message1);

        // Unfortunately greenmail can't handle multipart mime-mails properly (yet?).
        // This is why we use Test2.eml instead of Test.eml.
        InputStream is2 = IntegrationTest.class.getClassLoader().getResourceAsStream("mailbox/Test2.eml");
        MimeMessage message2 = new MimeMessage(null, is2);
        user.deliver(message2);
    }

    /**
     * Destroy test mailbox.
     */
    private void tearDownMailbox() throws Exception {
        if (greenMail != null) {
            greenMail.stop();
            greenMail = null;
        }
    }

    /**
     * Create a test user.
     */
    private void setUpUser() throws Exception {
        user = StaticAccessor.getUserAccessor().getUser(USERNAME);
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

    public void validateMailboxImap() throws Exception {
        IMAPStore imapStore = greenMail.getImaps().createStore();
        imapStore.connect(USERNAME, PASSWORD);

        assertEquals("Expected an empty mailbox.", 0, imapStore.getFolder("INBOX").getMessageCount());
        assertEquals("Not all messages ended up in the processed folder.", MESSAGE_COUNT, imapStore.getFolder("INBOX").getFolder("Processed").getMessageCount());
    }

    public void validateMailboxPop3() throws Exception {
        assertEquals("Expected an empty mailbox.", 0, greenMail.getReceivedMessages().length);
    }

    public void setUp() throws Exception {
        setUpMailbox();
        setUpConfiguration();
        setUpUser();
        setUpSpaces();
    }

    public void tearDown() throws Exception {
        try {
            tearDownSpaces();
        } finally {
            try {
                tearDownUser();
            } finally {
                tearDownMailbox();
            }
        }
    }

    public void testProcessImaps() throws Exception
    {
        try {
            setUp();

            // Save the configuration
            mailConfiguration.setProtocol("imap");
            mailConfiguration.setSecure(true);
            mailConfiguration.setPort(3993);
            mailConfiguration.setCheckCertificates(false);
            StaticAccessor.getMailConfigurationManager().saveConfig(mailConfiguration);

            // Reset global state, so that config gets fetched from disk
            StaticAccessor.getGlobalState().setMailConfigurationWrapper(null);

            // Sleep 7 minutes, in that time the mail2blog job needs to run
            TimeUnit.MINUTES.sleep(7);

            validatePosts();
            validateMailboxImap();
        } finally {
            tearDown();
        }
    }

    public void testProcessPop3() throws Exception
    {
        try {
            setUp();

            // Save the configuration
            mailConfiguration.setProtocol("pop3");
            mailConfiguration.setSecure(false);
            mailConfiguration.setPort(3110); // greenmail imap test port
            StaticAccessor.getMailConfigurationManager().saveConfig(mailConfiguration);

            // Reset global state, so that config gets fetched from disk.
            StaticAccessor.getGlobalState().setMailConfigurationWrapper(null);

            // Sleep 7 minutes, in that time the mail2blog job needs to run
            TimeUnit.MINUTES.sleep(7);

            validatePosts();
            validateMailboxPop3();
        } finally {
            tearDown();
        }
    }
}
