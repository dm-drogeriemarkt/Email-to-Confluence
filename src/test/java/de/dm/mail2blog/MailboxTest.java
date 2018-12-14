package de.dm.mail2blog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MailboxTest {

    private MailboxTestMockData mockData;
    @Mock IMailboxFlagFeature iMailboxFlagFeature;
    @Mock Message message ;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
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

        mailbox.close();
        verify(mockData.getStore(), times(1)).close();
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

        mailbox.close();
        verify(mockData.getStore(), times(1)).close();
    }

    @Test
    public void testCount() throws Exception {
        Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(MailConfiguration.builder().build())));
        doReturn(mockData.getStore()).when(mailbox).getStore();
        assertEquals("Expected 2 messages in INBOX", 2, mailbox.getCount());
    }

    @Test
    public void testGetMessages() throws Exception {
        Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(MailConfiguration.builder().build())));
        doReturn(mockData.getStore()).when(mailbox).getStore();

        Message[] messages = mailbox.getMessages();
        assertEquals("Expected 2 messages in INBOX", 2, messages.length);
        assertEquals("Failed to get first message from INBOX", mockData.getExampleMail1().getSubject(), messages[0].getSubject());
        assertEquals("Failed to get second message from INBOX", mockData.getExampleMail2().getSubject(), messages[1].getSubject());
    }

    @Test
    public void testGetDefaultFolder() throws Exception {
        Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(MailConfiguration.builder().build())));
        doReturn(mockData.getStore()).when(mailbox).getStore();

        assertEquals(mockData.getDefaultFolder(), mailbox.getDefaultFolder());
        verify(mockData.getDefaultFolder(), times(1)).open(Folder.READ_WRITE);

        mailbox.close();
        verify(mockData.getDefaultFolder(), times(1)).close(true);
    }

    @Test
    public void testGetInbox() throws Exception {
        Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(MailConfiguration.builder().build())));
        doReturn(mockData.getStore()).when(mailbox).getStore();

        assertEquals(mockData.getInbox(), mailbox.getInbox());
        verify(mockData.getInbox(), times(1)).open(Folder.READ_WRITE);

        mailbox.close();
        verify(mockData.getInbox(), times(1)).close(true);
    }

    @Test
    public void testGetFlaggingStrategyPop3() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
                .protocol("pop3")
                .build();
        Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(mailConfiguration)));
        IMailboxFlagFeature strategy = mailbox.getFlagStrategy();
        assertTrue(strategy instanceof Pop3MailboxFlagStrategy);
    }

    @Test
    public void testGetFlaggingStrategyImap() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
                .protocol("imap")
                .build();
        Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(mailConfiguration)));
        IMailboxFlagFeature strategy = mailbox.getFlagStrategy();
        assertTrue(strategy instanceof ImapMailboxFlagStrategy);
    }

    @Test
    public void testFlagAsProcessed() throws Exception {
        Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(MailConfiguration.builder().build())));
        doReturn(iMailboxFlagFeature).when(mailbox).getFlagStrategy();

        mailbox.flagAsProcessed(message);
        verify(iMailboxFlagFeature, times(1)).flagAsProcessed(message);
    }

    @Test
    public void testFlagAsInvalid() throws Exception {
        Mailbox mailbox = spy(new Mailbox(new MailConfigurationWrapper(MailConfiguration.builder().build())));
        doReturn(iMailboxFlagFeature).when(mailbox).getFlagStrategy();

        mailbox.flagAsInvalid(message);
        verify(iMailboxFlagFeature, times(1)).flagAsInvalid(message);
    }
}

