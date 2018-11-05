package de.dm.mail2blog;

import com.atlassian.xwork.ParameterSafe;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import de.dm.mail2blog.base.ContentTypes;
import de.dm.mail2blog.base.SpaceRule;
import lombok.*;

import java.util.Scanner;

/**
 * Bean that stores the mail configuration.
 */
@Builder(toBuilder=true)
@ParameterSafe // https://developer.atlassian.com/confdev/confluence-plugin-guide/confluence-plugin-module-types/xwork-webwork-module/xwork-plugin-complex-parameters-and-security
@JsonDeserialize(builder = MailConfiguration.MailConfigurationBuilder.class)
@EqualsAndHashCode
@Data
public class MailConfiguration {
    @NonNull private String server;
    @NonNull private String protocol; // 'IMAP' or 'POP3'.
    @NonNull private int port;
    @NonNull private String emailaddress;
    @NonNull private String username;
    @NonNull private String password;
    @NonNull private boolean secure; // Use IMAPs or POP3s
    @NonNull private boolean checkCertificates; // Check SSL certificates
    @NonNull private String sslVersions; // force SSL/TLS version to use
    @NonNull private String defaultSpace;
    @NonNull private String defaultContentType;

    // If set to true, the gallerymacro will be added to posts containing images.
    @NonNull private boolean gallerymacro;

    // If set to true, content will be wrapped into the html macro.
    // The html macro must be enabled.
    @NonNull private boolean htmlmacro;

    // Included for backwords compability with v1, got replaced with spaceRules.
    @NonNull private boolean spaceKeyInAddress;
    @NonNull private boolean spaceKeyInSubject;

    @NonNull @Getter(onMethod=@__({@ParameterSafe})) private SpaceRule[] spaceRules;

    // List of preferred content types to use.
    // There are preferred in the order of the list.
    @NonNull private String[] preferredContentTypes;

    // To prevent data loss of users. The configuration page normally displays
    // a warning pop up to users, when they use POP3. This popup can be disabled with this flag.
    @NonNull private boolean doNotShowPop3Confirmation = false;

    // The rules to use to filter HTML in mails.
    // The plugin uses the owasp.html framework to filter html.
    // We store them as boolean flags to be compatible with xstream/bandana storage
    // and to set/get them easily from velocity templates.
    // Using an ArrayList as storage proofed to be too troublesome.
    // Use getHtmlFilter() to get a policyFactory.
    @NonNull private boolean htmlFilterFormatting;
    @NonNull private boolean htmlFilterBlocks;
    @NonNull private boolean htmlFilterImages;
    @NonNull private boolean htmlFilterLinks;
    @NonNull private boolean htmlFilterStyles;
    @NonNull private boolean htmlFilterTables;

    // The maximum allowed size for an attachment.
    @NonNull private int maxAllowedAttachmentSize;

    // The maximum allowed number of attachments.
    // If set to -1 the number isn't limited.
    @NonNull private int maxAllowedNumberOfAttachments;

    // Restrict accepted sender addresses to confluence users in this groups.
    // Empty string -> accept all mail addresses.
    @NonNull private String securityGroup;

    // Store the list of allowed file types as a string to be compatible
    // with xstream/bandana storage and to edit it with a textfield on the administration page.
    @NonNull private String allowedFileTypes;

    // Builder class with default values.
    @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MailConfigurationBuilder
    {
        private String server = "";
        private String protocol = "imap"; // Default IMAPs
        private int port = 993;
        private boolean secure = true;
        private boolean checkCertificates = true;
        private String sslVersions = "";
        private String emailaddress = "";
        private String username = "";
        private String password = "";
        private String defaultSpace = "";
        private String defaultContentType = ContentTypes.BlogPost;
        private boolean gallerymacro = false;
        private boolean htmlmacro = false;
        private boolean spaceKeyInAddress = false;
        private boolean spaceKeyInSubject = false;
        private SpaceRule[] spaceRules = new SpaceRule[]{};
        private String[] preferredContentTypes = new String[]{"text/html", "application/xhtml+xml", "text/plain"};
        private boolean htmlFilterFormatting = true;
        private boolean htmlFilterBlocks = true;
        private boolean htmlFilterImages = true;
        private boolean htmlFilterLinks = true;
        private boolean htmlFilterStyles = true;
        private boolean htmlFilterTables = true;
        private int maxAllowedAttachmentSize = 100;
        private int maxAllowedNumberOfAttachments = -1;
        private String securityGroup = "";
        private String allowedFileTypes = new Scanner(
            getClass().getClassLoader().getResourceAsStream("filetypes.txt"),
            "UTF-8"
        ).useDelimiter("\\A").next();
    }
}
