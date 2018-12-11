package ut.de.dm.mail2blog;

import de.dm.mail2blog.Mailbox;
import de.dm.mail2blog.Pop3MailboxFlagStrategy;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.mail.Flags;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class Pop3MailboxFlagStrategyTest {

    @Mock
    Mailbox mailbox;
    MailboxTestMockData mockData;
    Pop3MailboxFlagStrategy strategy;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        mockData = new MailboxTestMockData();
        when(mailbox.getStore()).thenReturn(mockData.getStore());
        when(mailbox.getInbox()).thenReturn(mockData.getInbox());
        strategy = new Pop3MailboxFlagStrategy(mailbox);
    }

    /**
     * Test flagging on Pop3.
     */
    @Test
    public void testFlaggingPop3() throws Exception {
        // Flag messages.
        strategy.flagAsProcessed(mockData.getExampleMail1());
        strategy.flagAsInvalid(mockData.getExampleMail2());

        verify(mockData.getExampleMail1(), times(1)).setFlag(Flags.Flag.DELETED, true);
        verify(mockData.getExampleMail2(), times(1)).setFlag(Flags.Flag.DELETED, true);
    }
}
