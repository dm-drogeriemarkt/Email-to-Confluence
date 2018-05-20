package de.dm.mail2blog;

import com.atlassian.confluence.core.DefaultSaveContext;
import com.atlassian.confluence.pages.*;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.EntityException;
import com.atlassian.user.Group;
import com.atlassian.user.GroupManager;
import com.atlassian.user.User;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

/**
 * Converts email messages to Confluence pages/blog posts.
 */
@Slf4j
public class MessageToContentProcessor {
    /**
     * Configuration to use.
     */
    @NonNull @Setter private MailConfigurationWrapper mailConfigurationWrapper;

    /**
     * Create a new processor with given mail configuration.
     * @param mailConfigurationWrapper the config to use.
     */
    public MessageToContentProcessor(MailConfigurationWrapper mailConfigurationWrapper)
    throws MailConfigurationManagerException
    {
        this.mailConfigurationWrapper = mailConfigurationWrapper;
    }

    /**
     * Create a page or blog post from the content and the attachments retrieved from a mail message.
     *
     * @param space A list of spaces into which to post.
     * @param message The Email to process.
     * @param contentType ContentType to create
     */
    public void process(Space space, Message message, String contentType)
    throws MessageToContentProcessorException
    {
        try {
            MessageParser parser = newMessageParser(message, mailConfigurationWrapper);

            // Get sender.
            User sender = parser.getSender();

            // Validate sender.
            checkSender(sender);

            List<MailPartData> mailData = parser.getContent();

            // Create the blogPost and add values.
            AbstractPage page;
            if (ContentTypes.Page.equals(contentType)) {
                page = new Page();
            } else if (ContentTypes.BlogPost.equals(contentType)) {
                    page = new BlogPost();
            } else {
                throw new MessageToContentProcessorException("invalid content contentType");
            }

            // Set the creation date of the page/blog post to the current date.
            page.setCreationDate(new Date());

            // Set the space where to save the page/blog post.
            page.setSpace(space);

            // Generate page/blog post.
            String content = "";

            // Add html.
            for (MailPartData data : mailData) {
                if (data.getHtml() != null) {
                    content += "<p>" + data.getHtml() + "</p>";
                }
            }

            // Set creator.
            page.setCreator((sender instanceof ConfluenceUser) ? (ConfluenceUser)sender : null);

            // Set the title.
            page.setTitle(generateTitle(message, page));

            // Save the page/blog post.
            getPageManager().saveContentEntity(page, new DefaultSaveContext());

            boolean found_attachments = false;
            // Link attachments with page/blog post and set creator.
            // we have to save the page/blog post before we can add the attachments,
            // because attachments need to be attached to a content.
            try {
                for (MailPartData data: mailData) {
                    if (data.getAttachment() != null) {
                        data.getAttachment().setContainer(page);
                        data.getAttachment().setCreator((sender instanceof ConfluenceUser) ? (ConfluenceUser)sender : null);

                        getAttachmentManager().saveAttachment(data.getAttachment(), null, data.getStream());
                        found_attachments = true;
                    }
                }
            } catch (Exception e) {
                log.debug("Mail2Blog: Failed to save attachment: " + e.getMessage(), e);
            }

            // Fix cid links to attachments.
            // By going through attachments and replacing all cid links in text.
            for (MailPartData data: mailData) {
                if (data.getAttachment() != null && data.getContentID() != null) {
                    String url = getSettingsManager().getGlobalSettings().getBaseUrl() + data.getAttachment().getDownloadPath();
                    String cid = data.getContentID().replaceFirst("^.*<", "").replaceFirst(">.*$", "");
                    content = content.replace("cid:" + cid, url);
                }
            }

            // Filter the html.
            content = HtmlFilterFactory.makeHtmlFilter(mailConfigurationWrapper).sanitize(content);

            // Wrap html in html-macro, if html macro is enabled.
            if (mailConfigurationWrapper.getMailConfiguration().getHtmlmacro()) {
                content =
                "<ac:structured-macro ac:name=\"html\">" +
                    "<ac:plain-text-body>" +
                        "<![CDATA[" +
                            content +
                        "]]>" +
                    "</ac:plain-text-body>" +
                "</ac:structured-macro>";
            }

            // Add gallerymacro, if set in config and the post contains images.
            if (mailConfigurationWrapper.getMailConfiguration().getGallerymacro()) {
                boolean postContainsImages = false;
                for (MailPartData data : mailData) {
                    if (data.getContentType().toLowerCase().startsWith("image")) {
                        postContainsImages = true;
                        break;
                    }
                }

                if (postContainsImages) {
                    content = "<p><ac:structured-macro ac:name=\"gallery\"/></p>" + content;
                }
            }

            // Add list of attachment to the end of the page/blog post.
            if (found_attachments) {
                content += "<h3>Attachments</h3>";
                content += "<ul>";
                for (MailPartData data: mailData) {
                    if (data.getAttachment() != null) {
                        String title = data.getAttachment().getDisplayTitle();
                        String url = getSettingsManager().getGlobalSettings().getBaseUrl() + data.getAttachment().getDownloadPath();
                        content += "<li><a href=\"" + escapeHtml(url) + "\">" + escapeHtml(title) + "</a></li>";
                    }
                }
                content += "</ul>";
            }

            log.debug("Mail2Blog: Page/Blog entry content converted:\n" + content);

            // Set content.
            page.setBodyAsString(content);

        } catch (MessageParserException e) {
            throw new MessageToContentProcessorException(e);
        }
    }

/**
 * Check that the sender has permission to post.
 *
 * @param sender the user object for the sender
 * @throws MessageToContentProcessorException
 *  If the user hasn't permission to post. Or the check fails.
 */
private void checkSender(User sender)
    throws MessageToContentProcessorException
    {
        try {
            if (
                mailConfigurationWrapper.getMailConfiguration().getSecurityGroup() != null
                && !mailConfigurationWrapper.getMailConfiguration().getSecurityGroup().isEmpty()
            ) {
                if (sender == null) {
                    throw new MessageToContentProcessorException("Could not find a confluence user for sender address.");
                }

                Group group = getGroupManager().getGroup(mailConfigurationWrapper.getMailConfiguration().getSecurityGroup());
                if (group == null) {
                    throw new MessageToContentProcessorException("Invalid group in settings.");
                }

                if (!getGroupManager().hasMembership(group, sender)) {
                    throw new MessageToContentProcessorException("Sender mail lacks permissions to create pages/blog posts.");
                }
            }
        } catch (EntityException e) {
            throw new MessageToContentProcessorException("Failed to check the group membership of the sender.", e);
        }
    }

    /**
     * Generate a title for a page, from an email message.
     * Tries to use the subject of the message and adds characters to the title to make
     * sure that the title is unique in the current space.
     *
     * @param message The email to get the subject from
     * @param page The page/blog post to generate the title for (used to get the creation date and space).
     *
     */
    private String generateTitle(Message message, AbstractPage page) throws MessageToContentProcessorException {
        String title = "";

        try {
            title = message.getSubject();
        } catch (MessagingException me) {
            log.warn("Could not get subject from message.");
            title = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
        }

        // Check if the title is already in use.
        // Copied from @see atlassian.confluence.pages.DefaultPageManager.throwIfDuplicateAbstractPageTitle().
        // If we try to save page/the blog post with a title that is already used a runtime exception will
        // be thrown, that resets the transaction. We must avoid this.
        boolean titleUsed = false;

        int i = 0;
        do {
            Calendar publishDateCalendar = Calendar.getInstance();
            if (page.getCreationDate() != null) {
                publishDateCalendar.setTime(page.getCreationDate());
            }

            if (page instanceof BlogPost) {
                titleUsed = null != getPageManager().getBlogPost(
                        page.getSpaceKey(),
                        title,
                        publishDateCalendar
                );
            } else {
                 titleUsed = null != getPageManager().getPage(
                        page.getSpaceKey(),
                        title
                );
            }

            // Append a plus to the title 3 times.
            // If that isn't enough to generate a uniq title append a uniq id.
            if (titleUsed && i < 3) {
                title += "+";
            } else if (titleUsed && i >= 3) {
                title += " - " + UUID.randomUUID().toString();
            }

            // If something goes terribly wrong.
            if (i > 4) {
                throw new MessageToContentProcessorException("Failed to generate a unique title for this page/blog post. Aborting.");
            }

            i++;
        } while(titleUsed);

        return title;
    }

    public MessageParser newMessageParser(Message message, MailConfigurationWrapper mailConfigurationWrapper) {
        MessageParser parser =  new MessageParser(message, mailConfigurationWrapper);
        ContainerManager.autowireComponent(parser);
        return parser;
    }

    public AttachmentManager getAttachmentManager() {
        return StaticAccessor.getAttachmentManager();
    }

    public PageManager getPageManager() {
        return StaticAccessor.getPageManager();
    }

    public GroupManager getGroupManager() {
        return StaticAccessor.getGroupManager();
    }

    public SettingsManager getSettingsManager() {
        return StaticAccessor.getSettingsManager();
    }
}
