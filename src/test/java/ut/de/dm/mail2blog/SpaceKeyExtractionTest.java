package ut.de.dm.mail2blog;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.json.jsonorg.JSONObject;
import de.dm.mail2blog.MailConfiguration;
import de.dm.mail2blog.MailConfigurationWrapper;
import de.dm.mail2blog.SpaceKeyExtractor;
import de.dm.mail2blog.SpaceRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SpaceKeyExtractionTest {

    @Mock private SpaceManager spaceManager;
    private SpaceKeyExtractor spaceKeyExtractor;

    @Before
    public void setUp() throws Exception
    {
        // Mock spaceManger.
        when(spaceManager.getSpace(anyString())).thenReturn(mock(Space.class));

        // Create spaceKeyExtractor.
        spaceKeyExtractor = spy(new SpaceKeyExtractor());
        doReturn(spaceManager).when(spaceKeyExtractor).getSpaceManager();
    }

    /**
     * Test that the default space is found.
     */
    @Test
    public void testDefaultSpace() {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .defaultSpace("DefaultSpace")
        .build();

        Message message = mock(Message.class);
        List<Space> spaces = spaceKeyExtractor.getSpaces(new MailConfigurationWrapper(mailConfiguration), message);

        assertEquals("Expected to find one space (the default space)", 1, spaces.size());
        verify(spaceManager).getSpace("DefaultSpace");
    }

    /**
     * Test space rules.
     */
    @Test
    public void testSpaceRules() throws Exception {

        String[][] table = new String[][]{
            //            Field,     Operator,   Value,                  Action,     Space
            new String[]{ "from",    "is",       "alice@example.org",    "copy",     "marseille", },
            new String[]{ "to",      "contains", "shop",                 "move",     "paris",     },
            new String[]{ "cc",      "start",    "alice@",               "copy",     "grenoble",  },
            new String[]{ "to/cc",   "end",      "@example.org",         "copy",     "lyon",      },
            new String[]{ "subject", "regexp",   "[0-9]+",               "copy",     "bordeaux",  },
        };

        Object[][] messages = new Object[][]{
            //            From,                TO,                CC,                  Subject,    Spaces
            new Object[]{ "alice@example.org", "info@shop.de",    "alice@example.com", "test123",  new String[]{ "marseille", "paris" } },
            new Object[]{ "alice@example.org", "bob@example.org", "alice@example.org", "test123",  new String[]{ "marseille", "grenoble", "lyon", "bordeaux", "defaultSpace" } },
        };

        SpaceRule[] spaceRules = new SpaceRule[table.length];
        for (int i = 0; i < table.length; i++) {
            spaceRules[i] = SpaceRule.builder()
                .field(table[i][0])
                .operator(table[i][1])
                .value(table[i][2])
                .action(table[i][3])
                .space(table[i][4])
                .build();
        }

        MailConfiguration mailConfiguration = MailConfiguration.builder()
            .spaceRules(spaceRules)
            .defaultSpace("defaultSpace")
        .build();

        for (Object[] m : messages) {
            SpaceManager spaceManager = mock(SpaceManager.class);
            when(spaceManager.getSpace(anyString())).thenReturn(mock(Space.class));
            doReturn(spaceManager).when(spaceKeyExtractor).getSpaceManager();

            String from        = (String)m[0];
            String to          = (String)m[1];
            String cc          = (String)m[2];
            String subject     = (String)m[3];
            String[] spaceKeys = (String[])m[4];

            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("from", from);
            jsonMessage.put("to", to);
            jsonMessage.put("cc", cc);
            jsonMessage.put("subject", subject);

            // Create message
            Message message = mock(Message.class);
            when(message.getFrom()).thenReturn(new Address[] { new InternetAddress(from) });
            when(message.getRecipients(Message.RecipientType.TO)).thenReturn(new Address[] { new InternetAddress(to) });
            when(message.getRecipients(Message.RecipientType.CC)).thenReturn(new Address[] { new InternetAddress(cc) });
            when(message.getSubject()).thenReturn(subject);

            // Try to get spaces
            List<Space> spaces = spaceKeyExtractor.getSpaces(new MailConfigurationWrapper(mailConfiguration), message);

            // Check that the right number of spaces where created.
            assertEquals("Wrong number of spaces found for Message" + jsonMessage.toString(), spaceKeys.length, spaces.size());
            for (String spaceKey: spaceKeys) {
                verify(spaceManager).getSpace(spaceKey);
            }
        }

        Address[] recipientsTo = new Address[] {
            new InternetAddress("alice+london@example.org"),
            new InternetAddress("bob+paris@example.org"),
        };
        Message message = mock(Message.class);
    }

    /**
     * Test that the old spaceKeyInSubject option still works with SpaceRules.
     */
    @Test
    public void testSpaceKeyFromSubject() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .spaceKeyInSubject(true)
        .build();

        Message message = mock(Message.class);
        when(message.getSubject()).thenReturn("SubjectSpace: Hello World");

        List<Space> spaces = spaceKeyExtractor.getSpaces(new MailConfigurationWrapper(mailConfiguration), message);

        assertEquals("Expected to find one space (the space from subject)", 1, spaces.size());
        verify(spaceManager).getSpace("SubjectSpace");
    }

    /**
     * Test that the old spaceKeyInAddress option still works with SpaceRules.
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

        List<Space> spaces = spaceKeyExtractor.getSpaces(new MailConfigurationWrapper(mailConfiguration), message);

        assertEquals("Expected to find two spaces (one from TO-Header, one from CC-Header)", 2, spaces.size());
        verify(spaceManager).getSpace("paris");
        verify(spaceManager).getSpace("marseille");
    }
}

