package de.dm.mail2blog;

import lombok.Getter;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MailboxTestMockData {
    @Getter private MimeMessage exampleMail1;
    @Getter private MimeMessage exampleMail2;
    @Getter @Mock private Store store;
    @Getter @Mock private Folder inbox;
    @Getter @Mock private Folder defaultFolder;
    private boolean defaultFolderIsOpen = false;
    private boolean inboxIsOpen = false;

    MailboxTestMockData() throws MessagingException {
        initMocks(this);

        exampleMail1 = spy(new MimeMessage((Session)null));
        exampleMail1.setSubject("Mail1");
        exampleMail1.setFrom(new InternetAddress("alice@example.org"));
        exampleMail1.setText("Content1");
        exampleMail1.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress("bob@example.org"));

        exampleMail2 = spy(new MimeMessage((Session)null));
        exampleMail2.setSubject("Mail2");
        exampleMail2.setFrom(new InternetAddress("alice@example.org"));
        exampleMail2.setText("Content2");
        exampleMail2.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress("bob@example.org"));

        when(store.getFolder("INBOX")).thenReturn(inbox);
        when(inbox.getMessageCount()).thenReturn(2);
        when(inbox.getMessages()).thenReturn(new Message[]{exampleMail1, exampleMail2});
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return inboxIsOpen;
            }
        }).when(inbox).isOpen();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                inboxIsOpen = true;
                return null;
            }
        }).when(inbox).open(anyInt());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                inboxIsOpen = false;
                return null;
            }
        }).when(inbox).close(anyBoolean());

        when(store.getDefaultFolder()).thenReturn(defaultFolder);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return defaultFolderIsOpen;
            }
        }).when(defaultFolder).isOpen();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                defaultFolderIsOpen = true;
                return null;
            }
        }).when(defaultFolder).open(anyInt());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                defaultFolderIsOpen = false;
                return null;
            }
        }).when(defaultFolder).close(anyBoolean());

        when(exampleMail1.getFolder()).thenReturn(inbox);
        when(exampleMail2.getFolder()).thenReturn(inbox);
    }
}
