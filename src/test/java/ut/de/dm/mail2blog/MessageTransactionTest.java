package ut.de.dm.mail2blog;

import com.atlassian.confluence.spaces.Space;
import de.dm.mail2blog.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MessageTransaction.class, SpaceFactory.class})
@PowerMockIgnore("javax.security.auth.Subject")
public class MessageTransactionTest
{
    /**
     * Example mail message from resources/exampleMail.eml.
     */
    static Message exampleMessage;

    private MailConfigurationWrapper mailConfiguration;
    private Mailbox mailbox;
    private Space space;
    private MessageToBlogPostProcessor processor;
    private MessageTransaction messageTransaction;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Read in example mail from disk.
        InputStream is = MessageParserTest.class.getClassLoader().getResourceAsStream("exampleMail.eml");
        exampleMessage = new MimeMessage(null, is);
    }

    @Before
    public void setUp() throws Exception {
        mailbox = mock(Mailbox.class);
        space = mock(Space.class);

        mailConfiguration = new MailConfigurationWrapper(
            MailConfiguration.builder().username("alice").emailaddress("alice@example.org").build()
        );

        ArrayList<Space> spaces = new ArrayList<Space>();
        spaces.add(space);

        mockStatic(SpaceFactory.class);
        when(SpaceFactory.getSpace(mailConfiguration, exampleMessage)).thenReturn(spaces);

        processor = mock(MessageToBlogPostProcessor.class);

        mock(MessageToBlogPostProcessor.class);
        whenNew(MessageToBlogPostProcessor.class).withAnyArguments().thenReturn(processor);

        messageTransaction = MessageTransaction.builder()
            .message(exampleMessage)
            .mailbox(mailbox)
            .mailConfigurationWrapper(mailConfiguration)
            .build();
    }

    /**
     * Check that all process functions are called as expected,
     * when we pass valid params.
     */
    @Test
    public void testValidProcess() throws Exception {
        messageTransaction.doInTransaction();

        verify(processor).process(space, exampleMessage);
        verify(mailbox).flagAsProcessed(exampleMessage);
        verify(mailbox, never()).flagAsInvalid(any(Message.class));
    }

    /**
     * Check that processing functions aren't called when no space is found.
     * Check that message gets flagged as invalid.
     */
    @Test
    public void testNoSpace() throws Exception {
        when(SpaceFactory.getSpace(mailConfiguration, exampleMessage)).thenReturn(new ArrayList<Space>());
        messageTransaction.doInTransaction();

        verify(processor, never()).process(any(Space.class), any(Message.class));
        verify(mailbox).flagAsInvalid(exampleMessage);
        verify(mailbox, never()).flagAsProcessed(any(Message.class));
    }

    /**
     * Check that an exception coming from the processor is handled correctly.
     */
    @Test
    public void testProcessorError() throws Exception {
        doThrow(new MessageToBlogPostProcessorException()).when(processor).process(space, exampleMessage);
        messageTransaction.doInTransaction();

        verify(processor).process(space, exampleMessage);
        verify(mailbox).flagAsInvalid(exampleMessage);
        verify(mailbox, never()).flagAsProcessed(any(Message.class));
    }
}
