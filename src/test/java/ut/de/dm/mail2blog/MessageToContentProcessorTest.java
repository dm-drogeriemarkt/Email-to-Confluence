package ut.de.dm.mail2blog;

import com.atlassian.confluence.core.DefaultSaveContext;
import com.atlassian.confluence.pages.*;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.user.Group;
import com.atlassian.user.GroupManager;
import de.dm.mail2blog.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.mail.Message;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MessageToContentProcessorTest
{

    static final String BASE_URL = "http://example.org/";
    static final String ATTACHMENT_NAME = "image.gif";
    static final String ATTACHMENT_TYPE = "image/gif";
    static final String ATTACHMENT_ID = "IMAGE";
    static final String ATTACHMENT_URL = BASE_URL + ATTACHMENT_NAME;
    static final String MESSAGE_SUBJECT = "Hello World";
    static final String MESSAGE_KEYWORD = "LOOKFORME";
    static final String MESSAGE_CONTENT = "<h1>Hello World</h1><p>" + MESSAGE_KEYWORD + "<img src=\"cid:" + ATTACHMENT_ID + "\" /></p>";

    /**
     * Example mail message from resources/exampleMail.eml.
     */
    static Message exampleMessage;

    @Mock private AttachmentManager attachmentManager;
    @Mock private PageManager pageManager;
    @Mock private GroupManager groupManager;
    @Mock private SettingsManager settingsManager;
    @Mock private Settings globalSettings;
    @Mock private MessageParser messageParser;
    @Mock private Message message;
    @Mock private ConfluenceUser user;
    @Mock private Space space;
    @Mock private Attachment attachment;
    @Mock private Group group;

    private void autowire(MessageToContentProcessor processor) {
        doReturn(attachmentManager).when(processor).getAttachmentManager();
        doReturn(pageManager).when(processor).getPageManager();
        doReturn(groupManager).when(processor).getGroupManager();
        doReturn(settingsManager).when(processor).getSettingsManager();
    }

    @Before
    public void setUp() throws Exception
    {
        // Mock global base url.
        when(settingsManager.getGlobalSettings()).thenReturn(globalSettings);
        when(globalSettings.getBaseUrl()).thenReturn(BASE_URL);

        // Stub content to parse.
        ArrayList<MailPartData> content = new ArrayList<MailPartData>();

        MailPartData htmlPart = new MailPartData();
        htmlPart.setHtml(MESSAGE_CONTENT);
        htmlPart.setContentType("text/html");
        content.add(htmlPart);

        when(attachment.getDownloadPath()).thenReturn(ATTACHMENT_NAME);
        when(attachment.getDisplayTitle()).thenReturn(ATTACHMENT_NAME);
        MailPartData attachmentPart = new MailPartData();
        attachmentPart.setContentID("<" + ATTACHMENT_ID + ">");
        attachmentPart.setContentType(ATTACHMENT_TYPE);
        attachmentPart.setAttachment(attachment);
        attachmentPart.setStream(new ByteArrayInputStream(new byte[]{'a', 'b', 'c'}));
        content.add(attachmentPart);

        when(message.getSubject()).thenReturn(MESSAGE_SUBJECT);
        when(messageParser.getContent()).thenReturn(content);
        when(messageParser.getSender()).thenReturn(user);
    }

    /**
     * Test the creation of a blog post from message with default settings.
     */
    @Test
    public void testProcess() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder().build();

        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(new MailConfigurationWrapper(mailConfiguration)));
        doReturn(messageParser).when(processor).newMessageParser(any(Message.class), any(MailConfigurationWrapper.class));
        autowire(processor);

        // Process message.
        processor.process(space, message, ContentTypes.BlogPost);

        // Check that a blog post was saved and capture the saved blog post.
        ArgumentCaptor<BlogPost> argBlog = ArgumentCaptor.forClass(BlogPost.class);
        verify(pageManager).saveContentEntity(argBlog.capture(), isA(DefaultSaveContext.class));

        // Check block post.
        assertSame("Space not properly set on blog post", space, argBlog.getValue().getSpace());
        assertEquals("Blog post has wrong title", MESSAGE_SUBJECT, argBlog.getValue().getTitle());
        assertSame("Creator not properly set on blog post", user, argBlog.getValue().getCreator());

        // Check attachment.
        verify(attachment).setCreator(user);
        verify(attachmentManager).saveAttachment(same(attachment), eq((Attachment)null), isA(InputStream.class));

        // Check content.
        assertTrue("Content not added to body of blog post", argBlog.getValue().getBodyAsString().contains(MESSAGE_KEYWORD));

        String link = "<a href=\"" + ATTACHMENT_URL + "\">" + ATTACHMENT_NAME + "</a>";
        String src = "src=\"" + ATTACHMENT_URL + "\"";
        assertTrue("Attachments not added to body of blog post", argBlog.getValue().getBodyAsString().contains(link));
        assertTrue("Link to attachment not fixed in body of blog post", argBlog.getValue().getBodyAsString().contains(src));

        // Check absence of macros.
        assertFalse(
            "Gallery macro should not get added to blog post",
            argBlog.getValue().getBodyAsString().contains("<ac:structured-macro ac:name=\"gallery\"")
        );

        assertFalse(
            "Html macro should not get added to blog post",
            argBlog.getValue().getBodyAsString().contains("<ac:structured-macro ac:name=\"html\">")
        );
    }

    /**
     * Test that the gallery macro is added to the blog post if enabled.
     */
    @Test
    public void testGalleryMacro() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .gallerymacro(true)
        .build();

        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(new MailConfigurationWrapper(mailConfiguration)));
        doReturn(messageParser).when(processor).newMessageParser(any(Message.class), any(MailConfigurationWrapper.class));
        autowire(processor);

        // Process message.
        processor.process(space, message, ContentTypes.Page);

        // Capture the saved blog post.
        ArgumentCaptor<Page> argPage = ArgumentCaptor.forClass(Page.class);
        verify(pageManager).saveContentEntity(argPage.capture(), isA(DefaultSaveContext.class));

        assertTrue(
            "Gallery macro not added to blog post",
            argPage.getValue().getBodyAsString().contains("<ac:structured-macro ac:name=\"gallery\"")
        );
    }

    /**
     * Test that the html macro is added to the blog post if enabled.
     */
    @Test
    public void testHtmlMacro() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .htmlmacro(true)
        .build();

        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(new MailConfigurationWrapper(mailConfiguration)));
        doReturn(messageParser).when(processor).newMessageParser(any(Message.class), any(MailConfigurationWrapper.class));
        autowire(processor);

        // Process message.
        processor.process(space, message, ContentTypes.BlogPost);

        // Capture the saved blog post.
        ArgumentCaptor<BlogPost> argBlog = ArgumentCaptor.forClass(BlogPost.class);
        verify(pageManager).saveContentEntity(argBlog.capture(), isA(DefaultSaveContext.class));

        assertTrue(
            "Html macro not added to blog post",
            argBlog.getValue().getBodyAsString().contains("<ac:structured-macro ac:name=\"html\">")
        );
    }

    /**
     * Check that anonymous can't post if a security group is configured.
     */
    @Test
    public void testDisallowAnnonymous() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .securityGroup("confluence-users")
        .build();

        // Set user to null.
        user = null;

        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(new MailConfigurationWrapper(mailConfiguration)));
        doReturn(messageParser).when(processor).newMessageParser(any(Message.class), any(MailConfigurationWrapper.class));
        autowire(processor);

        // Try processing message.
        try {
            processor.process(space, message, ContentTypes.BlogPost);
            fail("No exception thrown");
        } catch (MessageToContentProcessorException e) {}
    }

    /**
     * Check that anonymous can post if a security group is configured.
     */
    @Test
    public void testAllowAnnonymous() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .securityGroup("")
        .build();

        // Set user to null.
        user = null;

        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(new MailConfigurationWrapper(mailConfiguration)));
        doReturn(messageParser).when(processor).newMessageParser(any(Message.class), any(MailConfigurationWrapper.class));
        autowire(processor);

        // Try processing message.
        try {
            processor.process(space, message, ContentTypes.BlogPost);
        } catch (MessageToContentProcessorException e) {
            fail("Failed to post");
        }
    }

    /**
     * Check that a user who is not in the security group isn't allowed to post.
     */
    @Test
    public void testUserNotInGroup() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .securityGroup("mail2blog")
        .build();

        when(groupManager.getGroup("mail2blog")).thenReturn(group);
        when(groupManager.hasMembership(group, user)).thenReturn(false);

        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(new MailConfigurationWrapper(mailConfiguration)));
        doReturn(messageParser).when(processor).newMessageParser(any(Message.class), any(MailConfigurationWrapper.class));
        autowire(processor);

        // Try processing message.
        try {
            processor.process(space, message, ContentTypes.BlogPost);
            fail("No exception thrown");
        } catch (MessageToContentProcessorException e) {}
    }

    /**
     * Check that a user who is in the security group can post.
     */
    @Test
    public void testUserInGroup() throws Exception {
        MailConfiguration mailConfiguration = MailConfiguration.builder()
        .securityGroup("confluence-administrators")
        .build();

        when(groupManager.getGroup("confluence-administrators")).thenReturn(group);
        when(groupManager.hasMembership(group, user)).thenReturn(true);

        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(new MailConfigurationWrapper(mailConfiguration)));
        doReturn(messageParser).when(processor).newMessageParser(any(Message.class), any(MailConfigurationWrapper.class));
        autowire(processor);

        // Try processing message.
        try {
            processor.process(space, message, ContentTypes.BlogPost);
        } catch (MessageToContentProcessorException e) {
            fail("Failed to post");
        }
    }
}
