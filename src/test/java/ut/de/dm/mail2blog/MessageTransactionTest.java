package ut.de.dm.mail2blog;

import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import de.dm.mail2blog.*;
import de.dm.mail2blog.base.ContentTypes;
import de.dm.mail2blog.base.Mail2BlogBaseConfiguration;
import de.dm.mail2blog.base.SpaceExtractor;
import de.dm.mail2blog.base.SpaceInfo;
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

    private Mail2BlogBaseConfiguration mail2BlogBaseConfiguration;
    private Mailbox mailbox;
    @Mock private MessageToContentProcessor processor;
    @Mock private SpaceExtractor spaceExtractor;
    private MessageTransaction messageTransaction;
    @Mock private Space space;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Read in example mail from disk.
        InputStream is = MessageTransactionTest.class.getClassLoader().getResourceAsStream("exampleMail.eml");
        exampleMessage = new MimeMessage(null, is);
    }

    @Before
    public void setUp() throws Exception {
        mailbox = mock(Mailbox.class);

        MailConfigurationWrapper mailConfigurationWrapper = spy(new MailConfigurationWrapper(
            MailConfiguration.builder().username("alice").emailaddress("alice@example.org").build()
        ));


        SettingsManager settingsManager = mock(SettingsManager.class);
        Settings globalSettings = mock(Settings.class);
        when(mailConfigurationWrapper.getSettingsManager()).thenReturn(settingsManager);
        when(settingsManager.getGlobalSettings()).thenReturn(globalSettings);
        when(globalSettings.getAttachmentMaxSize()).thenReturn((long)(1024 * 1024 * 100)); // 100 MB

        mail2BlogBaseConfiguration = mailConfigurationWrapper.getMail2BlogBaseConfiguration();

        ArrayList<SpaceInfo> spaces = new ArrayList<SpaceInfo>();
        spaces.add(SpaceInfo.builder().spaceKey("space").contentType(ContentTypes.BlogPost).build());

        when(spaceExtractor.getSpaces(any(Mail2BlogBaseConfiguration.class), eq(exampleMessage))).thenReturn(spaces);

        SpaceManager spaceManager = mock(SpaceManager.class);
        when(spaceManager.getSpace("space")).thenReturn(space);

        messageTransaction = spy(MessageTransaction.builder()
            .spaceManager(spaceManager)
            .spaceExtractor(spaceExtractor)
            .message(exampleMessage)
            .mailbox(mailbox)
            .mailConfigurationWrapper(mailConfigurationWrapper)
            .build());

        doReturn(processor).when(messageTransaction).newMessageToBlogProcessor(mailConfigurationWrapper);
    }

    /**
     * Check that all process functions are called as expected,
     * when we pass valid params.
     */
    @Test
    public void testValidProcess() throws Exception {
        messageTransaction.doInTransaction();

        verify(processor).process(space, exampleMessage, ContentTypes.BlogPost);
        verify(mailbox).flagAsProcessed(exampleMessage);
        verify(mailbox, never()).flagAsInvalid(any(Message.class));
    }

    /**
     * Check that processing functions aren't called when no space is found.
     * Check that message gets flagged as invalid.
     */
    @Test
    public void testNoSpace() throws Exception {
        when(spaceExtractor.getSpaces(any(Mail2BlogBaseConfiguration.class), any(Message.class))).thenReturn(new ArrayList<SpaceInfo>());
        messageTransaction.doInTransaction();

        verify(processor, never()).process(any(Space.class), any(Message.class), any(String.class));
        verify(mailbox).flagAsInvalid(exampleMessage);
        verify(mailbox, never()).flagAsProcessed(any(Message.class));
    }

    /**
     * Check that an exception coming from the processor is handled correctly.
     */
    @Test
    public void testProcessorError() throws Exception {
        doThrow(new MessageToContentProcessorException()).when(processor).process(space, exampleMessage, ContentTypes.BlogPost);
        messageTransaction.doInTransaction();

        verify(processor).process(space, exampleMessage, ContentTypes.BlogPost);
        verify(mailbox).flagAsInvalid(exampleMessage);
        verify(mailbox, never()).flagAsProcessed(any(Message.class));
    }
}
