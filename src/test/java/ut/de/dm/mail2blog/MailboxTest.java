package ut.de.dm.mail2blog;

import de.dm.mail2blog.MailConfiguration;
import de.dm.mail2blog.MailConfigurationWrapper;
import de.dm.mail2blog.Mailbox;
import de.saly.javamail.mock2.MailboxFolder;
import de.saly.javamail.mock2.MockMailbox;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class MailboxTest {

    MimeMessage exampleMail1;
    MimeMessage exampleMail2;
    MockMailbox mockMailbox;

    @Before
    public void setUp() throws Exception
    {
        MockMailbox.resetAll();

        exampleMail1 = new MimeMessage((Session)null);
        exampleMail1.setSubject("Mail1");
        exampleMail1.setFrom(new InternetAddress("alice@example.org"));
        exampleMail1.setText("Content1");
        exampleMail1.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress("bob@example.org"));

        exampleMail2 = new MimeMessage((Session)null);
        exampleMail2.setSubject("Mail2");
        exampleMail2.setFrom(new InternetAddress("alice@example.com"));
        exampleMail2.setText("Content2");
        exampleMail2.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress("bob@example.org"));

        mockMailbox = MockMailbox.get("bob@example.org");
        MailboxFolder inbox = mockMailbox.getInbox();
        inbox.add(exampleMail1);
        inbox.add(exampleMail2);
    }

    private MailConfiguration.MailConfigurationBuilder getBasicConfiguration() {
        return MailConfiguration.builder()
        .server("mail.example.org")
        .secure(false)
        .username("bob@example.org")
        .password("password")
        .protocol("mock_pop3")
        .port(110)
        .emailaddress("bob@example.org");
    }

    /**
     * Check that the number of messages in the inbox are correctly counted.
     */
    @Test
    public void testCount() throws Exception
    {
        MailConfiguration mailConfiguration = getBasicConfiguration().build();

        Mailbox mailbox = new Mailbox(new MailConfigurationWrapper(mailConfiguration));

        assertEquals("Expected 2 messages in INBOX", 2, mailbox.getCount());
    }

    /**
     * Check that the number of messages in the inbox are correctly counted.
     */
    @Test
    public void testGetMessages() throws Exception
    {
        MailConfiguration mailConfiguration = getBasicConfiguration().build();

        Mailbox mailbox = new Mailbox(new MailConfigurationWrapper(mailConfiguration));

        Message[] messages = mailbox.getMessages();
        assertEquals("Expected 2 messages in INBOX", 2, messages.length);
        assertEquals("Failed to get first message from INBOX", exampleMail1.getSubject(), messages[0].getSubject());
        assertEquals("Failed to get second message from INBOX", exampleMail2.getSubject(), messages[1].getSubject());
    }

    /**
     * Test flagging on IMAP.
     */
    @Test
    public void testFlaggingImap() throws Exception
    {
        MailConfiguration mailConfiguration = getBasicConfiguration()
        .protocol("mock_imap")
        .port(143)
        .build();

        Mailbox mailbox = new Mailbox(new MailConfigurationWrapper(mailConfiguration));

        Message[] messages = mailbox.getMessages();
        assertEquals("Expected 2 messages in INBOX", 2, messages.length);

        // Flag messages.
        mailbox.flagAsProcessed(messages[0]);
        mailbox.flagAsInvalid(messages[1]);

        Folder processed = mailbox.getInbox().getFolder("Processed");
        Folder invalid = mailbox.getInbox().getFolder("Invalid");

        assertNotNull("Failed to get processed folder", processed);
        assertNotNull("Failed to get invalid folder", invalid);

        processed.open(Folder.READ_ONLY);
        invalid.open(Folder.READ_ONLY);

        Message processedMessage = processed.getMessage(1);
        Message invalidMessage = invalid.getMessage(1);

        assertNotNull("No message in processed folder", processedMessage);
        assertNotNull("No message in invalid", invalidMessage);

        assertEquals("Failed to mark message as processed", exampleMail1.getSubject(), processedMessage.getSubject());
        assertEquals("Failed to mark message as invalid", exampleMail2.getSubject(), invalidMessage.getSubject());

        mailbox.close();

        mailbox = new Mailbox(new MailConfigurationWrapper(mailConfiguration));
        assertEquals("Expected 0 messages in INBOX", 0, mailbox.getCount());
    }

    /**
     * Test flagging on Pop3.
     */
    @Test
    public void testFlaggingPop3() throws Exception
    {
        MailConfiguration mailConfiguration = getBasicConfiguration()
        .protocol("mock_pop3")
        .port(110)
        .build();

        Mailbox mailbox = new Mailbox(new MailConfigurationWrapper(mailConfiguration));

        Message[] messages = mailbox.getMessages();
        assertEquals("Expected 2 messages in INBOX", 2, messages.length);

        // Flag messages.
        mailbox.flagAsProcessed(messages[0]);
        mailbox.flagAsInvalid(messages[1]);

        mailbox.close();

        mailbox = new Mailbox(new MailConfigurationWrapper(mailConfiguration));
        assertEquals("Expected 0 messages in INBOX", 0, mailbox.getCount());
    }
}

