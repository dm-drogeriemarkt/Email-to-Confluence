package ut.de.dm.mail2blog;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.user.User;
import com.atlassian.user.search.SearchResult;
import com.atlassian.user.search.page.Pager;
import de.dm.mail2blog.MailConfiguration;
import de.dm.mail2blog.MailConfigurationWrapper;
import de.dm.mail2blog.MailPartData;
import de.dm.mail2blog.MessageParser;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MessageParserTest
{
    /**
     * Example mail message from resources/exampleMail.eml.
     */
    static Message exampleMessage;

    @Mock private UserAccessor userAccessor;
    @Mock private SettingsManager settingsManager;
    @Mock private Settings globalSettings;
    @Mock private Attachment attachment;
    @Mock SearchResult searchResult;
    @Mock Pager pager;
    @Mock User user;
    @Mock ConfluenceUser confluenceUser;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Read in example mail from disk.
        InputStream is = MessageParserTest.class.getClassLoader().getResourceAsStream("exampleMail.eml");
        exampleMessage = new MimeMessage(null, is);
    }

    @Before
    public void setUp() throws Exception
    {
        // Mock global max attachment size.
        when(settingsManager.getGlobalSettings()).thenReturn(globalSettings);
        when(globalSettings.getAttachmentMaxSize()).thenReturn((long)(1024 * 1024 * 100)); // 100 MB
    }

    /**
     * Test that the charset is correctly returned from given header.
     */
    @Test
    public void testGetCharsetFromHeader()
    {
        MailConfiguration mailConfiguration = MailConfiguration.builder().build();

        MessageParser messageParser = spy(new MessageParser(exampleMessage, new MailConfigurationWrapper(mailConfiguration)));

        Charset utf8 = messageParser.getCharsetFromHeader("text/plain; charset=\"utf-8\"");
        Charset iso88591 = messageParser.getCharsetFromHeader("text/html; charset=\"ISO-8859-1\"");
        Charset cp437 = messageParser.getCharsetFromHeader("text/plain; charset=CP437");
        Charset missing = messageParser.getCharsetFromHeader("text/plain");
        Charset invalid = messageParser.getCharsetFromHeader("text/plain; charset=xxx123");

        assertEquals(Charset.forName("utf-8"), utf8);
        assertEquals(Charset.forName("ISO-8859-1"), iso88591);
        assertEquals(Charset.forName("CP437"), cp437);
        assertEquals("Expected default charset, when charset is missing from content-type.", Charset.defaultCharset(), missing);
        assertEquals("Expected default charset, when an invalid/unknown charset is given in content-type.", Charset.defaultCharset(), invalid);
    }

    /**
     * Check that the content of the example mail is properly extracted.
     * When using the default values.
     */
    @Test
    public void testExtractionWithDefaults() throws Exception
    {
        MailConfiguration mailConfiguration = MailConfiguration.builder().build();

        MessageParser messageParser = spy(new MessageParser(exampleMessage, new MailConfigurationWrapper(mailConfiguration)));
        doReturn(mock(Attachment.class)).when(messageParser).newAttachment();
        doReturn(settingsManager).when(messageParser).getSettingsManager();

        // Extract data from message.
        List<MailPartData> content = messageParser.getContent();

        assertEquals("Expected two parts in mail", 2, content.size());

        // Get html and attachment.
        MailPartData htmlPart = content.get(0);
        MailPartData attachment = content.get(1);

        assertEquals("Wrong mimeType for html part", "text/html", htmlPart.getContentType());
        assertTrue("Could not find <p>Lieber Bob,</p> in html", htmlPart.getHtml().contains("<p>Lieber Bob,</p>"));

        assertEquals("Wrong content-id for attachment", "<6FA75120-9E1A-45DE-9001-620110B831AD>", attachment.getContentID());
        assertEquals("Wrong mimeType for attachment", "image/gif", attachment.getContentType());
        verify(attachment.getAttachment()).setFileName("dm-logo.gif");
        verify(attachment.getAttachment()).setMediaType("image/gif");
        verify(attachment.getAttachment()).setFileSize(2155);
    }

    /**
     * Check that text is extracted instead of html if the option is used.
     */
    @Test
    public void testExtractionWithPreferredText() throws Exception
    {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .preferredContentTypes(new String[] {"text/plain", "text/html"})
        .build();

        MessageParser messageParser = spy(new MessageParser(exampleMessage, new MailConfigurationWrapper(mailConfiguration)));
        doReturn(mock(Attachment.class)).when(messageParser).newAttachment();
        doReturn(settingsManager).when(messageParser).getSettingsManager();

        // Extract data from message.
        List<MailPartData> content = messageParser.getContent();

        assertEquals("Expected two parts in mail", 2, content.size());

        // Get text part.
        MailPartData textPart = content.get(0);

        assertEquals("Wrong mimeType for text part", "text/plain", textPart.getContentType());
        assertTrue("Could not find - ALANA in text", textPart.getHtml().contains("- ALANA"));
    }

    /**
     * Check that no attachment is added if gif image/gif is not in allowed file types.
     */
    @Test
    public void testExtractionWithForbiddenMimeType() throws Exception
    {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .allowedFileTypes("jpg image/jpg")
        .build();

        MessageParser messageParser = spy(new MessageParser(exampleMessage, new MailConfigurationWrapper(mailConfiguration)));

        // Extract data from message.
        List<MailPartData> content = messageParser.getContent();

        for (MailPartData part : content) {
            assertNull("Expected no attachment. Buf found one.", part.getAttachment());
        }

    }

    /**
     * Check that no attachment is added if the max number of allowed attachments is 0.
     */
    @Test
    public void testExtractionWithMaxNumberOfAllowedAttachments() throws Exception
    {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .maxAllowedNumberOfAttachments(0)
        .build();

        MessageParser messageParser = spy(new MessageParser(exampleMessage, new MailConfigurationWrapper(mailConfiguration)));

        // Extract data from message.
        List<MailPartData> content = messageParser.getContent();

        for (MailPartData part : content) {
            assertNull("Expected no attachment. Buf found one.", part.getAttachment());
        }

    }

    /**
     * Check that no attachment is added if the maximum size for an attachment is 0 mb.
     */
    @Test
    public void testExtractionWithMaxAttachmentSize() throws Exception
    {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .maxAllowedAttachmentSize(0)
        .build();

        MessageParser messageParser = spy(new MessageParser(exampleMessage, new MailConfigurationWrapper(mailConfiguration)));
        doReturn(settingsManager).when(messageParser).getSettingsManager();

        // Extract data from message.
        List<MailPartData> content = messageParser.getContent();

        for (MailPartData part : content) {
            assertNull("Expected no attachment. But found one.", part.getAttachment());
        }
    }

    /**
     * Test getting the user from a mail message.
     *
     * Assuming userAccessor will return a ConfluenceUser. See:
     * https://github.com/dm-drogeriemarkt/Mail2Blog/issues/2
     */
    @Test
    public void testGetUserWithConfluenceUser() throws Exception
    {
        when(userAccessor.getUsersByEmail("alice@example.org")).thenReturn(searchResult);
        when(searchResult.pager()).thenReturn(pager);
        List<User> users = new ArrayList<User>();
        users.add(confluenceUser);
        when(pager.getCurrentPage()).thenReturn(users);

         MailConfiguration mailConfiguration = MailConfiguration.builder()
        .maxAllowedAttachmentSize(0)
        .build();

        MessageParser messageParser = spy(new MessageParser(exampleMessage, new MailConfigurationWrapper(mailConfiguration)));
        doReturn(userAccessor).when(messageParser).getUserAccessor();

        assertEquals("Failed to get user", confluenceUser, messageParser.getSender());
    }

    /**
     * Test getting the user from a mail message.
     *
     * Assuming userAccessor will return a ConfluenceUser. See:
     * https://github.com/dm-drogeriemarkt/Mail2Blog/issues/2
     */
    @Test
    public void testGetUserWithUser() throws Exception
    {
        when(userAccessor.getUsersByEmail("alice@example.org")).thenReturn(searchResult);
        when(searchResult.pager()).thenReturn(pager);
        List<User> users = new ArrayList<User>();
        users.add(user);
        when(pager.getCurrentPage()).thenReturn(users);

        when(user.getName()).thenReturn("alice");
        when(userAccessor.getUserByName("alice")).thenReturn(confluenceUser);

         MailConfiguration mailConfiguration = MailConfiguration.builder()
        .maxAllowedAttachmentSize(0)
        .build();

        MessageParser messageParser = spy(new MessageParser(exampleMessage, new MailConfigurationWrapper(mailConfiguration)));
        doReturn(userAccessor).when(messageParser).getUserAccessor();

        assertEquals("Failed to get user", confluenceUser, messageParser.getSender());
    }

    /**
     * Test getting an unknown user from a mail message.
     *
     * Assuming userAccessor will return a general User. See:
     * https://github.com/dm-drogeriemarkt/Mail2Blog/issues/2
     */
    @Test
    public void testGetUserUnknown() throws Exception
    {
        when(userAccessor.getUsersByEmail("alice@example.org")).thenReturn(searchResult);
        when(searchResult.pager()).thenReturn(pager);
        List<User> users = new ArrayList<User>();
        when(pager.getCurrentPage()).thenReturn(users);

         MailConfiguration mailConfiguration = MailConfiguration.builder()
        .maxAllowedAttachmentSize(0)
        .build();

        MessageParser messageParser = spy(new MessageParser(exampleMessage, new MailConfigurationWrapper(mailConfiguration)));
        doReturn(userAccessor).when(messageParser).getUserAccessor();

        assertNull("User returned instead of null", messageParser.getSender());
    }
}