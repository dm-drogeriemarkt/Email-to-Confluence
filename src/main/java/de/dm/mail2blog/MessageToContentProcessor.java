package de.dm.mail2blog;

import com.atlassian.confluence.core.DefaultSaveContext;
import com.atlassian.confluence.pages.*;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.EntityException;
import com.atlassian.user.Group;
import com.atlassian.user.GroupManager;
import com.atlassian.user.User;
import com.atlassian.user.search.SearchResult;
import com.atlassian.user.search.page.Pager;
import de.dm.mail2blog.base.*;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.text.SimpleDateFormat;
import java.util.*;

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
     * @param space       A list of spaces into which to post.
     * @param message     The Email to process.
     * @param contentType ContentType to create
     */
    public void process(Space space, Message message, String contentType)
            throws MessageToContentProcessorException, FileTypeBucketException {
        try {

            Mail2BlogBaseConfiguration mail2BlogBaseConfiguration = mailConfigurationWrapper.getMail2BlogBaseConfiguration();
            MessageParser parser = newMessageParser(message, mail2BlogBaseConfiguration);

            // Get sender.
            User sender = getSender(parser.getSenderEmail());

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
            page.setCreator((sender instanceof ConfluenceUser) ? (ConfluenceUser) sender : null);

            // Set the title.
            page.setTitle(generateTitle(message, page));

            // Save the page/blog post.
            getPageManager().saveContentEntity(page, new DefaultSaveContext());

            HashMap<MailPartData, Attachment> attachments = new HashMap<>();

            // Link attachments with page/blog post and set creator.
            // we have to save the page/blog post before we can add the attachments,
            // because attachments need to be attached to a content.
            try {
                for (MailPartData data : mailData) {
                    if (data.getAttachementData() != null) {
                        Attachment attachment = newAttachment();
                        attachment.setFileName(data.getAttachementData().getFilename());
                        attachment.setMediaType(data.getAttachementData().getMediaType());
                        attachment.setCreationDate(data.getAttachementData().getCreationDate());
                        attachment.setLastModificationDate(data.getAttachementData().getLastModificationDate());
                        attachment.setFileSize(data.getAttachementData().getFileSize());
                        attachment.setContainer(page);
                        attachment.setCreator((sender instanceof ConfluenceUser) ? (ConfluenceUser) sender : null);

                        getAttachmentManager().saveAttachment(attachment, null, data.getStream());
                        attachments.put(data, attachment);
                    }
                }
            } catch (Exception e) {
                log.debug("Mail2Blog: failed to save attachment", e);
            }

            // Fix cid links to attachments.
            // By going through attachments and replacing all cid links in text.
            for (MailPartData data : mailData) {
                if (attachments.get(data) != null && data.getContentID() != null) {
                    String url = getSettingsManager().getGlobalSettings().getBaseUrl() + attachments.get(data).getDownloadPath();
                    String cid = data.getContentID().replaceFirst("^.*<", "").replaceFirst(">.*$", "");
                    content = content.replace("cid:" + cid, url);
                }
            }

            // Filter the html.
            content = HtmlFilterFactory.makeHtmlFilter(mail2BlogBaseConfiguration).sanitize(content);

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
            if (attachments.size() > 0) {
                content += "<h3>Attachments</h3>";
                content += "<ul>";
                for (MailPartData data : mailData) {
                    if (attachments.get(data) != null && data.getContentID() != null) {
                        String title = attachments.get(data).getDisplayTitle();
                        String url = getSettingsManager().getGlobalSettings().getBaseUrl() + attachments.get(data).getDownloadPath();
                        content += "<li><a href=\"" + escapeHtml(url) + "\">" + escapeHtml(title) + "</a></li>";
                    }
                }
                content += "</ul>";
            }

            log.debug("Mail2Blog: page/blog entry content converted:\n" + content);

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
     * @throws MessageToContentProcessorException If the user hasn't permission to post. Or the check fails.
     */
    private void checkSender(User sender)
            throws MessageToContentProcessorException {
        try {
            if (
                    mailConfigurationWrapper.getMailConfiguration().getSecurityGroup() != null
                            && !mailConfigurationWrapper.getMailConfiguration().getSecurityGroup().isEmpty()
            ) {
                if (sender == null) {
                    throw new MessageToContentProcessorException("could not find a confluence user for sender address");
                }

                Group group = getGroupManager().getGroup(mailConfigurationWrapper.getMailConfiguration().getSecurityGroup());
                if (group == null) {
                    throw new MessageToContentProcessorException("invalid group in settings");
                }

                if (!getGroupManager().hasMembership(group, sender)) {
                    throw new MessageToContentProcessorException("sender mail lacks permissions to create pages/blog posts");
                }
            }
        } catch (EntityException e) {
            throw new MessageToContentProcessorException("failed to check the group membership of the sender", e);
        }
    }

    /**
     * Generate a title for a page, from an email message.
     * Tries to use the subject of the message and adds characters to the title to make
     * sure that the title is unique in the current space.
     *
     * @param message The email to get the subject from
     * @param page    The page/blog post to generate the title for (used to get the creation date and space).
     */
    private String generateTitle(Message message, AbstractPage page) throws MessageToContentProcessorException {
        String title = "";

        try {
            title = message.getSubject();
        } catch (MessagingException me) {
            log.warn("could not get subject from message");
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
                throw new MessageToContentProcessorException("failed to generate a unique title for this page/blog post, aborting");
            }

            i++;
        } while (titleUsed);

        return title;
    }

    /**
     * Get a confluence user for the sender of this e-mail message
     *
     * @return the Confluence or null if no user could be identified.
     */
    public ConfluenceUser getSender(String mail) {
        if (mail == null) { return null; }

        // Get user for mail.
        SearchResult result = getUserAccessor().getUsersByEmail(mail);
        Pager pager = result.pager();
        List page = pager.getCurrentPage();

        if (page.size() < 1) {
            return null;
        }

        if (!(page.get(0) instanceof User)) {
            return null;
        }

        // In recent confluence versions userAccessor.getUsersByEmail sometimes doesn't
        // seem to return ConfluenceUsers. In this case try to load the confluence user
        // from the userAccessor via the username.
        // Issue: https://github.com/dm-drogeriemarkt/Mail2Blog/issues/2.
        if (page.get(0) instanceof ConfluenceUser) {
            return (ConfluenceUser) page.get(0);
        } else {
            String username = ((User) page.get(0)).getName();
            return getUserAccessor().getUserByName(username);
        }
    }

    public MessageParser newMessageParser(Message message, Mail2BlogBaseConfiguration mail2BlogBaseConfiguration) {
        MessageParser parser = new MessageParser(message, mail2BlogBaseConfiguration);
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

    public UserAccessor getUserAccessor() {
        return StaticAccessor.getUserAccessor();
    }

    public Attachment newAttachment() {
        return new Attachment();
    }
}
