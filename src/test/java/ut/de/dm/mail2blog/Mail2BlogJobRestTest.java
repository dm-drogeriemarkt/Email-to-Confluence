package ut.de.dm.mail2blog;

import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.scheduler.status.RunOutcome;
import de.dm.mail2blog.Mail2BlogJob;
import de.dm.mail2blog.Mail2BlogJobRest;
import de.dm.mail2blog.Mail2BlogJobRestResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Mail2BlogJobRestTest
{
    private Mail2BlogJob mail2BlogJob;
    private Mail2BlogJobRest restAction;

    private final static String MESSAGE = "Hello World";
    private final static String STATUS = "SUCCESS";

    @Before
    public void setUp() {
        mail2BlogJob = mock(Mail2BlogJob.class);

        JobRunnerResponse response = mock(JobRunnerResponse.class);
        RunOutcome runOutcome = mock(RunOutcome.class);
        when(response.getMessage()).thenReturn(MESSAGE);
        when(response.getRunOutcome()).thenReturn(runOutcome);
        when(runOutcome.name()).thenReturn(STATUS);

        when(mail2BlogJob.runJob(null)).thenReturn(response);

        restAction = new Mail2BlogJobRest();
        restAction.setMail2BlogJob(mail2BlogJob);
    }

    @Test
    public void test() throws Exception {
        Response result = restAction.run();

        assertTrue("Wrong type", result.getEntity() instanceof Mail2BlogJobRestResponse);
        assertEquals("Expected HTTP-Status-Code 200", 200, result.getStatus());
        assertEquals(MESSAGE, ((Mail2BlogJobRestResponse)result.getEntity()).getMessage());
        assertEquals(STATUS, ((Mail2BlogJobRestResponse)result.getEntity()).getStatus());
    }
}