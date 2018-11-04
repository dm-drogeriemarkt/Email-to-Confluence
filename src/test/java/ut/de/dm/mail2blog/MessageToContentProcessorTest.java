package ut.de.dm.mail2blog;

import com.atlassian.confluence.core.DefaultSaveContext;
import com.atlassian.confluence.pages.*;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.core.util.DateUtils;
import com.atlassian.user.Group;
import com.atlassian.user.GroupManager;
import com.atlassian.user.User;
import com.atlassian.user.search.SearchResult;
import com.atlassian.user.search.page.Pager;
import de.dm.mail2blog.MailConfiguration;
import de.dm.mail2blog.MailConfigurationWrapper;
import de.dm.mail2blog.MessageToContentProcessor;
import de.dm.mail2blog.MessageToContentProcessorException;
import de.dm.mail2blog.base.*;
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
import java.util.Date;
import java.util.List;

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
    static final long   ATTACHMENT_SIZE = 42l;
    static final Date   ATTACHMENT_CREATION_DATE = DateUtils.getDateDay(1989, 11, 9);
    static final Date   ATTACHMENT_MODIFICATION_DATE = DateUtils.getDateDay(1990, 10, 3);
    static final String MESSAGE_FROM = "alice@example.org";
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
    @Mock private Group group;
    @Mock private Attachment attachment;
    @Mock private UserAccessor userAccessor;
    @Mock SearchResult searchResult;
    @Mock Pager pager;

    private MailConfigurationWrapper mailConfigurationWrapper;

    private void autowire(MessageToContentProcessor processor) {
        doReturn(attachmentManager).when(processor).getAttachmentManager();
        doReturn(pageManager).when(processor).getPageManager();
        doReturn(groupManager).when(processor).getGroupManager();
        doReturn(settingsManager).when(processor).getSettingsManager();
        doReturn(userAccessor).when(processor).getUserAccessor();
        doReturn(messageParser).when(processor).newMessageParser(any(Message.class), any(Mail2BlogBaseConfiguration.class));
        doReturn(attachment).when(processor).newAttachment();
    }

    @Before
    public void setUp() throws Exception
    {
        // Mock global base url.
        when(settingsManager.getGlobalSettings()).thenReturn(globalSettings);
        when(globalSettings.getBaseUrl()).thenReturn(BASE_URL);
        when(globalSettings.getAttachmentMaxSize()).thenReturn(1024*1024*100L); // 100mb

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
        attachmentPart.setAttachementData(AttachementData.builder()
            .filename(ATTACHMENT_NAME)
            .mediaType(ATTACHMENT_TYPE)
            .fileSize(ATTACHMENT_SIZE)
            .lastModificationDate(ATTACHMENT_MODIFICATION_DATE)
            .creationDate(ATTACHMENT_CREATION_DATE)
        .build());
        attachmentPart.setStream(new ByteArrayInputStream(new byte[]{'a', 'b', 'c'}));
        content.add(attachmentPart);

        when(message.getSubject()).thenReturn(MESSAGE_SUBJECT);
        when(messageParser.getSenderEmail()).thenReturn(MESSAGE_FROM);
        when(messageParser.getContent()).thenReturn(content);

        when(userAccessor.getUsersByEmail(MESSAGE_FROM)).thenReturn(searchResult);
        when(searchResult.pager()).thenReturn(pager);
        List<User> users = new ArrayList<User>();
        users.add(user);
        when(pager.getCurrentPage()).thenReturn(users);

        mailConfigurationWrapper = spy(new MailConfigurationWrapper(MailConfiguration.builder().build()));
        when(mailConfigurationWrapper.getSettingsManager()).thenReturn(settingsManager);
    }

    /**
     * Test the creation of a blog post from message with default settings.
     */
    @Test
    public void testProcess() throws Exception {
        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(mailConfigurationWrapper));
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
        verify(attachment).setFileName(ATTACHMENT_NAME);
        verify(attachment).setMediaType(ATTACHMENT_TYPE);
        verify(attachment).setFileSize(ATTACHMENT_SIZE);
        verify(attachment).setCreationDate(ATTACHMENT_CREATION_DATE);
        verify(attachment).setLastModificationDate(ATTACHMENT_MODIFICATION_DATE);
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
        mailConfigurationWrapper.getMailConfiguration().setGallerymacro(true);

        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(mailConfigurationWrapper));
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
        mailConfigurationWrapper.getMailConfiguration().setHtmlmacro(true);

        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(mailConfigurationWrapper));
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
        mailConfigurationWrapper.getMailConfiguration().setSecurityGroup("confluence-users");

        // Set user to null.
        user = null;

        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(mailConfigurationWrapper));
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
        mailConfigurationWrapper.getMailConfiguration().setSecurityGroup("");

        // Set user to null.
        user = null;

        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(mailConfigurationWrapper));
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
        mailConfigurationWrapper.getMailConfiguration().setSecurityGroup("mail2blog");

        when(groupManager.getGroup("mail2blog")).thenReturn(group);
        when(groupManager.hasMembership(group, user)).thenReturn(false);

        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(mailConfigurationWrapper));
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
        mailConfigurationWrapper.getMailConfiguration().setSecurityGroup("confluence-administrators");

        when(groupManager.getGroup("confluence-administrators")).thenReturn(group);
        when(groupManager.hasMembership(group, user)).thenReturn(true);

        // Generate processor.
        MessageToContentProcessor processor = spy(new MessageToContentProcessor(mailConfigurationWrapper));
        autowire(processor);

        // Try processing message.
        try {
            processor.process(space, message, ContentTypes.BlogPost);
        } catch (MessageToContentProcessorException e) {
            fail("Failed to post");
        }
    }
}
