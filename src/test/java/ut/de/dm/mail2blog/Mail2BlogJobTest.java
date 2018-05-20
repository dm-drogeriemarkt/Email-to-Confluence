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
import org.mockito.junit.MockitoJUnitRunner;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
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
        mail2BlogJob = spy(new Mail2BlogJob());

        transactionTemplate = mock(TransactionTemplate.class);
        doReturn(transactionTemplate).when(mail2BlogJob).getTransactionTemplate();
        mail2BlogJob.setSpaceExtractor(new SpaceExtractor());

        globalState = mock(GlobalState.class);
        mail2BlogJob.setGlobalState(globalState);

        mailConfiguration = MailConfiguration.builder().username("alice").emailaddress("alice@example.org").build();
        when(globalState.getMailConfigurationWrapper()).thenReturn(new MailConfigurationWrapper(mailConfiguration));

        // Mock mailbox.
        mailbox = mock(Mailbox.class);
        when(mailbox.getMessages()).thenReturn(new Message[]{exampleMessage});
        mock(Mailbox.class);

        doReturn(mailbox).when(mail2BlogJob).newMailbox(any(MailConfigurationWrapper.class));
    }

    /**
     * Make sure the job doesn't run with fetch mail disabled.
     */
    @Test
    public void testFetchDisabled() throws Exception {
        doReturn("true").when(mail2BlogJob).systemGetProperty("atlassian.mail.fetchdisabled");

        JobRunnerResponse response = mail2BlogJob.runJob(null);

        assertEquals("Expected run to abort", RunOutcome.ABORTED, response.getRunOutcome());
    }

    /**
     * Make sure the job doesn't run with pop mail disabled.
     */
    @Test
    public void testPopDisabled() throws Exception {
        doReturn("true").when(mail2BlogJob).systemGetProperty("atlassian.mail.popdisabled");

        JobRunnerResponse response = mail2BlogJob.runJob(null);

        assertEquals("Expected run to abort", RunOutcome.ABORTED, response.getRunOutcome());
    }

    /**
     * Make sure the transaction get's created and executed.
     */
    @Test
    public void testRunJob() throws Exception {
        doReturn("false").when(mail2BlogJob).systemGetProperty("atlassian.mail.fetchdisabled");
        doReturn("false").when(mail2BlogJob).systemGetProperty("atlassian.mail.popdisabled");

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
        doReturn("false").when(mail2BlogJob).systemGetProperty("atlassian.mail.fetchdisabled");
        doReturn("false").when(mail2BlogJob).systemGetProperty("atlassian.mail.popdisabled");

        doThrow(new MailboxException()).when(mailbox).getMessages();

        JobRunnerResponse response = mail2BlogJob.runJob(null);

        assertEquals("Expected run to fail", RunOutcome.FAILED, response.getRunOutcome());
    }
}
