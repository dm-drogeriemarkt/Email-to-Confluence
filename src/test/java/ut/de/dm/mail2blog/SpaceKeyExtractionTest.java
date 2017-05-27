package ut.de.dm.mail2blog;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.spring.container.ContainerManager;
import de.dm.mail2blog.MailConfiguration;
import de.dm.mail2blog.MailConfigurationWrapper;
import de.dm.mail2blog.SpaceFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ContainerManager.class})
public class SpaceKeyExtractionTest {

    @Mock private SpaceManager spaceManager;

    @Before
    public void setUp() throws Exception
    {
        // Mock ContainerManager.
        mockStatic(ContainerManager.class);
        when(ContainerManager.getComponent("spaceManager")).thenReturn(spaceManager);

        // Mock spaceManger.
        when(spaceManager.getSpace(anyString())).thenReturn(mock(Space.class));
    }

    /**
     * Test that the default space is found by SpaceFactory.
     */
    @Test
    public void testDefaultSpace() {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .defaultSpace("DefaultSpace")
        .build();

        Message message = mock(Message.class);
        List<Space> spaces = SpaceFactory.getSpace(new MailConfigurationWrapper(mailConfiguration), message);

        assertEquals("Expected to find one space (the default space)", 1, spaces.size());
        verify(spaceManager).getSpace("DefaultSpace");
    }

    /**
     * Test that a space from subject can be extracted.
     */
    @Test
    public void testSpaceKeyFromSubject() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .spaceKeyInSubject(true)
        .build();

        Message message = mock(Message.class);
        when(message.getSubject()).thenReturn("SubjectSpace: Hello World");

        List<Space> spaces = SpaceFactory.getSpace(new MailConfigurationWrapper(mailConfiguration), message);

        assertEquals("Expected to find one space (the space from subject)", 1, spaces.size());
        verify(spaceManager).getSpace("SubjectSpace");
    }

    /**
     * Test that a space from email in VERP format can be extracted.
     */
    @Test
    public void testSpaceKeyVERP() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .emailaddress("bob@example.org")
        .spaceKeyInAddress(true)
        .build();


        Message message = mock(Message.class);

        // Add one space in TO header.
        Address[] recipientsTo = new Address[] {
            new InternetAddress("alice+london@example.org"),
            new InternetAddress("bob+paris@example.org"),
        };
        when(message.getRecipients(Message.RecipientType.TO)).thenReturn(recipientsTo);

        // Add another space in CC: header.
        Address[] recipientsCC = new Address[] {
            new InternetAddress("bob+marseille@example.org"),
            new InternetAddress("andrew+york@example.org"),
        };
        when(message.getRecipients(Message.RecipientType.CC)).thenReturn(recipientsCC);

        List<Space> spaces = SpaceFactory.getSpace(new MailConfigurationWrapper(mailConfiguration), message);

        assertEquals("Expected to find two spaces (one from TO-Header, one from CC-Header)", 2, spaces.size());
        verify(spaceManager).getSpace("paris");
        verify(spaceManager).getSpace("marseille");
    }
}

