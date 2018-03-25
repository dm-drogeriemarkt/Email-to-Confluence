package ut.de.dm.mail2blog;

import com.atlassian.confluence.spaces.Space;
import de.dm.mail2blog.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MessageTransactionTest
{
    /**
     * Example mail message from resources/exampleMail.eml.
     */
    static Message exampleMessage;

    private MailConfigurationWrapper mailConfiguration;
    private Mailbox mailbox;
    private Space space;
    @Mock private MessageToBlogPostProcessor processor;
    @Mock private SpaceKeyExtractor spaceKeyExtractor;
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

        when(spaceKeyExtractor.getSpaces(mailConfiguration, exampleMessage)).thenReturn(spaces);

        messageTransaction = spy(MessageTransaction.builder()
            .spaceKeyExtractor(spaceKeyExtractor)
            .message(exampleMessage)
            .mailbox(mailbox)
            .mailConfigurationWrapper(mailConfiguration)
            .build());

        doReturn(processor).when(messageTransaction).newMessageToBlogProcessor(mailConfiguration);
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
        when(spaceKeyExtractor.getSpaces(mailConfiguration, exampleMessage)).thenReturn(new ArrayList<Space>());
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
