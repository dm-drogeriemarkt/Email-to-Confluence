package de.dm.mail2blog;


import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ImapMailboxFlagStrategyTest {

    @Mock Mailbox mailbox;
    MailboxTestMockData mockData;
    ImapMailboxFlagStrategy strategy;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        mockData = new MailboxTestMockData();
        when(mailbox.getStore()).thenReturn(mockData.getStore());
        when(mailbox.getInbox()).thenReturn(mockData.getInbox());
        when(mailbox.getDefaultFolder()).thenReturn(mockData.getDefaultFolder());
        strategy = new ImapMailboxFlagStrategy(mailbox);
    }

    private void verifyMsgsProcessed(Folder processed, Folder invalid) throws MessagingException {
        verify(processed, times(1)).create(Folder.HOLDS_MESSAGES);
        verify(invalid, times(0)).create(anyInt());
        verify(processed, times(1)).open(Folder.READ_WRITE);
        verify(invalid, times(1)).open(Folder.READ_WRITE);
        verify(mockData.getInbox(), times(1)).copyMessages(new Message[]{mockData.getExampleMail1()}, processed);
        verify(mockData.getInbox(), times(0)).copyMessages(new Message[]{mockData.getExampleMail1()}, invalid);
        verify(mockData.getInbox(), times(1)).copyMessages(new Message[]{mockData.getExampleMail2()}, invalid);
        verify(mockData.getInbox(), times(0)).copyMessages(new Message[]{mockData.getExampleMail2()}, processed);
    }

    @Test
    public void testFlaggingImapInbox() throws Exception {
        Folder processed = mock(Folder.class);
        Folder invalid = mock(Folder.class);

        when(mockData.getInbox().getFolder("Processed")).thenReturn(processed);
        when(mockData.getInbox().getFolder("Invalid")).thenReturn(invalid);

        when(processed.exists()).thenReturn(false);
        when(invalid.exists()).thenReturn(true);
        when(processed.create(Folder.HOLDS_MESSAGES)).thenReturn(true);

        processed.open(Folder.READ_ONLY);
        invalid.open(Folder.READ_ONLY);

        // Flag messages.
        strategy.flagAsProcessed(mockData.getExampleMail1());
        strategy.flagAsInvalid(mockData.getExampleMail2());

        verifyMsgsProcessed(processed, invalid);
    }

    @Test
    public void testFlaggingImapDefaultFolder() throws Exception {
        Folder inboxProcessed = mock(Folder.class);
        Folder inboxInvalid = mock(Folder.class);
        Folder defaultProcessed = mock(Folder.class);
        Folder defaultInvalid = mock(Folder.class);

        when(mockData.getInbox().getFolder("Processed")).thenReturn(inboxProcessed);
        when(mockData.getInbox().getFolder("Invalid")).thenReturn(inboxInvalid);
        when(mockData.getDefaultFolder().getFolder("Processed")).thenReturn(defaultProcessed);
        when(mockData.getDefaultFolder().getFolder("Invalid")).thenReturn(defaultInvalid);

        when(inboxProcessed.exists()).thenReturn(false);
        when(inboxInvalid.exists()).thenReturn(false);
        given(inboxProcessed.create(Folder.HOLDS_MESSAGES)).willAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new MailboxException("failed");
            }
        });
        given(inboxInvalid.create(Folder.HOLDS_MESSAGES)).willAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new MailboxException("failed");
            }
        });

        when(defaultProcessed.exists()).thenReturn(false);
        when(defaultInvalid.exists()).thenReturn(true);
        when(defaultProcessed.create(Folder.HOLDS_MESSAGES)).thenReturn(true);

        // Flag messages.
        strategy.flagAsProcessed(mockData.getExampleMail1());
        strategy.flagAsInvalid(mockData.getExampleMail2());

        verifyMsgsProcessed(defaultProcessed, defaultInvalid);
    }
}
