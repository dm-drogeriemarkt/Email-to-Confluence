package ut.de.dm.mail2blog;

import de.dm.mail2blog.MailConfiguration;
import de.dm.mail2blog.MailConfigurationWrapper;
import de.dm.mail2blog.Mailbox;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Session;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MailboxTest {

    private MailboxTestMockData mockData;

    @Before
    public void setUp() throws Exception {
        mockData = new MailboxTestMockData();
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
        doReturn(session).when(mailbox).getSessionInstance(propertiesCaptor.capture(), (Authenticator) isNull());
        when(session.getStore("imaps")).thenReturn(mockData.getStore());

        mailbox.getStore();

        verify(mockData.getStore(), times(1)).connect("mail.example.org", "bob", "password");
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
        doReturn(session).when(mailbox).getSessionInstance(propertiesCaptor.capture(), (Authenticator) isNull());
        when(session.getStore("pop3")).thenReturn(mockData.getStore());

        mailbox.getStore();

        verify(mockData.getStore(), times(1)).connect("mail.example.org", "bob", "password");
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
    public void testCount() throws Exception {
        Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(MailConfiguration.builder().build())));
        doReturn(mockData.getStore()).when(mailbox).getStore();
        assertEquals("Expected 2 messages in INBOX", 2, mailbox.getCount());
    }

    /**
     * Check that the number of messages in the inbox are correctly counted.
     */
    @Test
    public void testGetMessages() throws Exception {
        Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(MailConfiguration.builder().build())));
        doReturn(mockData.getStore()).when(mailbox).getStore();

        Message[] messages = mailbox.getMessages();
        assertEquals("Expected 2 messages in INBOX", 2, messages.length);
        assertEquals("Failed to get first message from INBOX", mockData.getExampleMail1().getSubject(), messages[0].getSubject());
        assertEquals("Failed to get second message from INBOX", mockData.getExampleMail2().getSubject(), messages[1].getSubject());
    }
}

