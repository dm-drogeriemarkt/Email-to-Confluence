package ut.de.dm.mail2blog;

import de.dm.mail2blog.MailConfiguration;
import de.dm.mail2blog.MailConfigurationWrapper;
import de.dm.mail2blog.Mailbox;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MailboxTest {

    MimeMessage exampleMail1;
    MimeMessage exampleMail2;
    Store mockStore;
    Folder mockInbox;

    @Before
    public void setUp() throws Exception
    {
        exampleMail1 = spy(new MimeMessage((Session)null));
        exampleMail1.setSubject("Mail1");
        exampleMail1.setFrom(new InternetAddress("alice@example.org"));
        exampleMail1.setText("Content1");
        exampleMail1.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress("bob@example.org"));

        exampleMail2 = spy(new MimeMessage((Session)null));
        exampleMail2.setSubject("Mail2");
        exampleMail2.setFrom(new InternetAddress("alice@example.com"));
        exampleMail2.setText("Content2");
        exampleMail2.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress("bob@example.org"));

        mockStore = mock(Store.class);

        mockInbox = mock(Folder.class);
        when(mockStore.getFolder("INBOX")).thenReturn(mockInbox);
        when(mockInbox.getMessageCount()).thenReturn(2);
        when(mockInbox.getMessages()).thenReturn(new Message[]{exampleMail1, exampleMail2});

        when(exampleMail1.getFolder()).thenReturn(mockInbox);
        when(exampleMail2.getFolder()).thenReturn(mockInbox);
    }

    @Test
    public void testGetStoreImap() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .server("mail.example.org")
        .secure(true)
        .sslVersions("TLSv1.2")
        .checkCertificates(true)
        .username("bob")
        .password("password")
        .protocol("imap")
        .port(143)
        .emailaddress("bob@example.org")
        .build();

        Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(mailConfiguration)));
        final ArgumentCaptor<Properties> propertiesCaptor = ArgumentCaptor.forClass(Properties.class);

        Session session = mock(Session.class);
        doReturn(session).when(mailbox).getSessionInstance(propertiesCaptor.capture(), (Authenticator)isNull());
        when(session.getStore("imaps")).thenReturn(mockStore);

        mailbox.getStore();

        verify(mockStore, times(1)).connect("mail.example.org", "bob", "password");
        Properties properties = propertiesCaptor.getValue();
        assertEquals("10000", properties.getProperty("mail.imaps.connectiontimeout"));
        assertEquals("143", properties.getProperty("mail.imaps.port"));
        assertEquals("true", properties.getProperty("mail.imaps.ssl.checkserveridentity"));
        assertNull(properties.getProperty("mail.pop3.ssl.trust"));
        assertEquals("TLSv1.2", properties.getProperty("mail.imaps.ssl.protocols"));
    }

    @Test
    public void testGetStorePop3() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .server("mail.example.org")
        .secure(false)
        .username("bob")
        .password("password")
        .protocol("pop3")
        .port(1110)
        .emailaddress("bob@example.org")
        .build();

        Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(mailConfiguration)));
        final ArgumentCaptor<Properties> propertiesCaptor = ArgumentCaptor.forClass(Properties.class);

        Session session = mock(Session.class);
        doReturn(session).when(mailbox).getSessionInstance(propertiesCaptor.capture(), (Authenticator)isNull());
        when(session.getStore("pop3")).thenReturn(mockStore);

        mailbox.getStore();

        verify(mockStore, times(1)).connect("mail.example.org", "bob", "password");
        Properties properties = propertiesCaptor.getValue();
        assertEquals("10000", properties.getProperty("mail.pop3.connectiontimeout"));
        assertEquals("1110", properties.getProperty("mail.pop3.port"));
        assertEquals("false", properties.getProperty("mail.pop3.ssl.checkserveridentity"));
        assertEquals("*", properties.getProperty("mail.pop3.ssl.trust"));
    }

    /**
     * Check that the number of messages in the inbox are correctly counted.
     */
    @Test
    public void testCount() throws Exception
    {
        Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(MailConfiguration.builder().build())));
        doReturn(mockStore).when(mailbox).getStore();
        assertEquals("Expected 2 messages in INBOX", 2, mailbox.getCount());
    }

   /**
    * Check that the number of messages in the inbox are correctly counted.
    */
   @Test
   public void testGetMessages() throws Exception
   {
       Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(MailConfiguration.builder().build())));
       doReturn(mockStore).when(mailbox).getStore();

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
       Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(
           MailConfiguration.builder()
               .protocol("imap")
               .build()
           ))
       );
       doReturn(mockStore).when(mailbox).getStore();

       Folder processed = mock(Folder.class);
       Folder invalid = mock(Folder.class);

       when(mockInbox.getFolder("Processed")).thenReturn(processed);
       when(mockInbox.getFolder("Invalid")).thenReturn(invalid);

       when(processed.exists()).thenReturn(false);
       when(invalid.exists()).thenReturn(true);
       when(processed.create(Folder.HOLDS_MESSAGES)).thenReturn(true);

       processed.open(Folder.READ_ONLY);
       invalid.open(Folder.READ_ONLY);

       // Flag messages.
       mailbox.flagAsProcessed(exampleMail1);
       mailbox.flagAsInvalid(exampleMail2);

       verify(processed, times(1)).create(Folder.HOLDS_MESSAGES);
       verify(invalid, times(0)).create(anyInt());
       verify(processed, times(1)).open(Folder.READ_WRITE);
       verify(invalid, times(1)).open(Folder.READ_WRITE);
       verify(mockInbox, times(1)).copyMessages(new Message[]{exampleMail1}, processed);
       verify(mockInbox, times(0)).copyMessages(new Message[]{exampleMail1}, invalid);
       verify(mockInbox, times(1)).copyMessages(new Message[]{exampleMail2}, invalid);
       verify(mockInbox, times(0)).copyMessages(new Message[]{exampleMail2}, processed);
   }

   /**
    * Test flagging on Pop3.
    */
   @Test
   public void testFlaggingPop3() throws Exception
   {
       Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(
           MailConfiguration.builder()
               .protocol("pop3")
               .build()
           ))
       );

       // Flag messages.
       mailbox.flagAsProcessed(exampleMail1);
       mailbox.flagAsInvalid(exampleMail2);

       verify(exampleMail1, times(1)).setFlag(Flags.Flag.DELETED, true);
       verify(exampleMail2, times(1)).setFlag(Flags.Flag.DELETED, true);
   }
}

