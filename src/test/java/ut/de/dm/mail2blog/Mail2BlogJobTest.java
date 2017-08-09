package ut.de.dm.mail2blog;

import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.scheduler.status.RunOutcome;
import de.dm.mail2blog.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Mail2BlogJob.class)
@PowerMockIgnore("javax.security.auth.Subject")
public class Mail2BlogJobTest
{
    /**
     * Example mail message from resources/exampleMail.eml.
     */
    static Message exampleMessage;

    private Mail2BlogJob mail2BlogJob;
    private MailConfiguration mailConfiguration;
    private GlobalState globalState;
    private TransactionTemplate transactionTemplate;
    private Mailbox mailbox;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Read in example mail from disk.
        InputStream is = MessageParserTest.class.getClassLoader().getResourceAsStream("exampleMail.eml");
        exampleMessage = new MimeMessage(null, is);
    }

    @Before
    public void setUp() throws Exception {
        // Create mail2Blog with mocked dependency injections.
        mail2BlogJob = new Mail2BlogJob();

        transactionTemplate = mock(TransactionTemplate.class);
        mail2BlogJob.setTransactionTemplate(transactionTemplate);

        globalState = mock(GlobalState.class);
        mail2BlogJob.setGlobalState(globalState);

        mailConfiguration = MailConfiguration.builder().username("alice").emailaddress("alice@example.org").build();
        when(globalState.getMailConfigurationWrapper()).thenReturn(new MailConfigurationWrapper(mailConfiguration));

        // Mock mailbox.
        mailbox = mock(Mailbox.class);
        when(mailbox.getMessages()).thenReturn(new Message[]{exampleMessage});

        mock(Mailbox.class);
        whenNew(Mailbox.class).withAnyArguments().thenReturn(mailbox);
    }

    /**
     * Make sure the job doesn't run with fetch mail disabled.
     */
    @Test
    public void testFetchDisabled() throws Exception {
        mockStatic(System.class);
        when(System.getProperty("atlassian.mail.fetchdisabled")).thenReturn("true");

        JobRunnerResponse response = mail2BlogJob.runJob(null);

        assertEquals("Expected run to abort", RunOutcome.ABORTED, response.getRunOutcome());
    }

    /**
     * Make sure the job doesn't run with pop mail disabled.
     */
    @Test
    public void testPopDisabled() throws Exception {
        mockStatic(System.class);
        when(System.getProperty("atlassian.mail.popdisabled")).thenReturn("true");

        JobRunnerResponse response = mail2BlogJob.runJob(null);

        assertEquals("Expected run to abort", RunOutcome.ABORTED, response.getRunOutcome());
    }

    /**
     * Make sure the transaction get's created and executed.
     */
    @Test
    public void testRunJob() throws Exception {
        mockStatic(System.class);
        when(System.getProperty("atlassian.mail.popdisabled")).thenReturn("false");
        when(System.getProperty("atlassian.mail.fetchdisabled")).thenReturn("false");

        JobRunnerResponse response = mail2BlogJob.runJob(null);

        assertEquals("Expected run to succeed", RunOutcome.SUCCESS, response.getRunOutcome());

        ArgumentCaptor<MessageTransaction> captor = ArgumentCaptor.forClass(MessageTransaction.class);
        verify(transactionTemplate).execute(captor.capture());

        assertSame(mailbox, captor.getValue().getMailbox());
        assertSame(exampleMessage, captor.getValue().getMessage());
        assertEquals("alice", captor.getValue().getMailConfigurationWrapper().getMailConfiguration().getUsername());
        assertEquals("alice@example.org", captor.getValue().getMailConfigurationWrapper().getMailConfiguration().getEmailaddress());
    }

    /**
     * Test an exception during processing is handled correctly.
     */
    @Test
    public void testException() throws Exception {
        mockStatic(System.class);
        when(System.getProperty("atlassian.mail.popdisabled")).thenReturn("false");
        when(System.getProperty("atlassian.mail.fetchdisabled")).thenReturn("false");

        doThrow(new MailboxException()).when(mailbox).getMessages();

        JobRunnerResponse response = mail2BlogJob.runJob(null);

        assertEquals("Expected run to fail", RunOutcome.FAILED, response.getRunOutcome());
    }
}
