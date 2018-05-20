package de.dm.mail2blog;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.user.EntityException;
import com.atlassian.user.Group;
import com.atlassian.user.GroupManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConfigurationAction extends ConfluenceActionSupport {

    // Auto wired components.
    @Setter @Autowired          private MailConfigurationManager mailConfigurationManager;
    @Setter @Autowired          private ConfigurationActionState configurationActionState;
    @Getter @Setter @Autowired  private CheckboxTracker checkboxTracker;
    @Setter @Autowired          private GlobalState globalState;
    @Setter @ComponentImport    private SpaceManager spaceManager;
    @Setter @ComponentImport    private GroupManager groupManager;

    // Xwork can easily deserialize to array of strings, but not to arrays of arbitrary objects.
    // Therefore we store the properties of SpaceRule in here.
    @Setter private String[] spaceRuleFields       = new String[]{};
    @Setter private String[] spaceRuleOperators    = new String[]{};
    @Setter private String[] spaceRuleValues       = new String[]{};
    @Setter private String[] spaceRuleSpaces       = new String[]{};
    @Setter private String[] spaceRuleActions      = new String[]{};
    @Setter private String[] spaceRuleContentTypes = new String[]{};

    /**
     * Triggered when the user accesses the edit form for the first time.
     */
    public String doDefault() {
        return ConfluenceActionSupport.INPUT;
    }

    /**
     * Get the mailConfiguration currently being edited.
     */
    public MailConfiguration getMailConfiguration() {
        return configurationActionState.getMailConfigurationWrapper().getMailConfiguration();
    }

    /**
     * Validate form.
     */
    public void validate() {
        // Commit & reset checkbox values.
        getCheckboxTracker().commit(getMailConfiguration());
        getCheckboxTracker().reset();

        super.validate();

        // Check that the mail server is given and is a valid hostname.
        if (getMailConfiguration().getServer().isEmpty()) {
            addFieldError("mailConfiguration.server","Please enter a value");
            addActionError("Please enter a mail server");
        } else {
            try {
                InetAddress.getByName(getMailConfiguration().getServer());
            } catch (UnknownHostException e) {
                addFieldError("mailConfiguration.server","Please enter a valid hostname/ip");
                addActionError("Please enter a valid hostname/ip as mail server");
            }
        }

        // Check that a username is given.
        if (getMailConfiguration().getUsername().isEmpty()) {
            addFieldError("mailConfiguration.username", "Please enter a value");
            addActionError("Please enter a user name");
        }

        // Check that emailaddress is given.
        if (getMailConfiguration().getEmailaddress().isEmpty()) {
            addFieldError("mailConfiguration.emailaddress", "Please enter a value");
            addActionError("Please enter a mail address");
        }

        // Validate protocol.
        if (!getProtocols().containsKey(getMailConfiguration().getProtocol())) {
            addFieldError("mailConfiguration.protocol", "Please choose a valid value");
            addActionError("Please choose a valid protocol");
        }

        // Validate port.
        if (getMailConfiguration().getPort() < 0 || getMailConfiguration().getPort() > 0xFFFF) {
            addFieldError("mailConfiguration.port", "Please enter a value between 0 and 65535");
            addActionError("Please enter a port number between 0 and 65535");
        }

        // Validate default space.
        if (spaceManager.getSpace(getMailConfiguration().getDefaultSpace()) == null) {
            addFieldError("mailConfiguration.defaultSpace", "Please choose a valid value");
            addActionError("Please choose a valid default space");
        }

        // Validate default content type.
        if (!ContentTypes.validate(getMailConfiguration().getDefaultContentType())) {
            addFieldError("mailConfiguration.defaultContentType", "Please choose a valid value");
            addActionError("Please choose a valid default content type");
        }

        // Validate attachment limits.
        if (getMailConfiguration().getMaxAllowedAttachmentSize() > 2048 || getMailConfiguration().getMaxAllowedAttachmentSize() < 1) {
            addFieldError("mailConfiguration.maxAllowedAttachmentSize", "Please enter a value between 0 and 2048MB");
            addActionError("Please choose a maximum attachment size between 0 and 2048MB");
        }

        if (getMailConfiguration().getMaxAllowedNumberOfAttachments() < -1) {
            addFieldError("mailConfiguration.maxAllowedNumberOfAttachments", "Please enter a value larger than -1");
            addActionError("Please set the maximum number of attachments to at least -1");
        }

        // Create space rules and validate them.
        SpaceRule[] spaceRules = new SpaceRule[spaceRuleFields.length];

        if (
               (spaceRuleFields.length != spaceRuleOperators.length)
            || (spaceRuleFields.length != spaceRuleValues.length)
            || (spaceRuleFields.length != spaceRuleSpaces.length)
            || (spaceRuleFields.length != spaceRuleActions.length)
            || (spaceRuleFields.length != spaceRuleContentTypes.length)
        ) {
            addActionError("Invalid space rules");
            addFieldError("mailConfiguration.spaceRules", "Invalid space rules");
        } else {
            for (int i = 0; i < spaceRuleFields.length; i++) {
                String field = spaceRuleFields[i];
                String operator = spaceRuleOperators[i];
                String value = spaceRuleValues[i];
                String space = spaceRuleSpaces[i];
                String action = spaceRuleActions[i];
                String contentType = spaceRuleContentTypes[i];

                // Create space rule
                SpaceRule spaceRule = SpaceRule.builder()
                    .field(field)
                    .operator(operator)
                    .value(value)
                    .space(space)
                    .action(action)
                    .contentType(contentType)
                    .build();

                try {
                    spaceRule.validate(spaceManager);
                } catch (SpaceRuleValidationException e) {
                    addActionError("Space rule " + (i+1) + ": " + e.getMessage());
                    addFieldError("mailConfiguration.spaceRules", "Rule " + (i+1) + ": " + e.getMessage());
                }

                spaceRules[i] = spaceRule;
            }

            getMailConfiguration().setSpaceRules(spaceRules);
        }

        spaceRuleFields = new String[]{};
        spaceRuleOperators = new String[]{};
        spaceRuleValues = new String[]{};
        spaceRuleSpaces = new String[]{};
        spaceRuleActions = new String[]{};
        spaceRuleContentTypes = new String[]{};

        // Check that the syntax of the file types is valid.
        try {
            FileTypeBucket.fromString(getMailConfiguration().getAllowedFileTypes());
        } catch (FileTypeBucketException e) {
            addFieldError("mailConfiguration.allowedFileTypes", e.getMessage());
            addActionError("Please fix the errors in the allowed file types text area");
        }

        // Check that the group if given is valid.
        try {
            if (
                !getMailConfiguration().getSecurityGroup().isEmpty()
                && groupManager.getGroup(getMailConfiguration().getSecurityGroup()) == null
            ) {
                addFieldError("mailConfiguration.securityGroup", "Please choose a valid value");
                addActionError("Please choose a valid group");
            }
        } catch (EntityException e) {
            log.error("Mail2Blog: Failed to check group", e);
        }

        // Test the mailbox configuration by trying to connect to the mailbox.
        try {
            Mailbox mailbox = new Mailbox(configurationActionState.getMailConfigurationWrapper());
            mailbox.getCount();
        } catch (Exception e) {
            addActionError("Failed to connect to mailbox: " + e.getMessage());
        }
    }

    /**
     * Process submitted values. Save the configuration to bandana storage.
     */
    public String execute() throws Exception {
        try {
            // Save the configuration and update the global state.
            mailConfigurationManager.saveConfig(getMailConfiguration());
            globalState.setMailConfigurationWrapper(configurationActionState.getMailConfigurationWrapper().duplicate());
            addActionMessage("Configuration successfully saved");
            return ConfluenceActionSupport.SUCCESS;
        } catch (MailConfigurationManagerException e) {
            addActionError("Failed to save configuration");
            log.error("Mail2Blog: Failed to save configuration", e);
            return ConfluenceActionSupport.ERROR;
        }
    }

    public String getPreferred() {
        if ("text/plain".equals(getMailConfiguration().getPreferredContentTypes()[0])) {
            return "text";
        } else {
            return "html";
        }
    }

    public void setPreferred(String contenttype) {
        if (contenttype.equals("text")) {
            getMailConfiguration().setPreferredContentTypes(new String[]{"text/plain", "text/html", "application/xhtml+xml"});
        } else {
            getMailConfiguration().setPreferredContentTypes(new String[]{"text/html", "application/xhtml+xml", "text/plain"});
        }
    }

    /**
     * Get a list with the possible protocols.
     *
     * @return Returns a map with the possible protocols.
     */
    public Map<String, String> getProtocols() {
        Hashtable<String, String> h = new Hashtable<String, String>();
        h.put("imap", "IMAP");
        h.put("pop3", "POP3");
        return h;
    }

    /**
     * Get a list with possible spaces.
     *
     * @return Returns a map with possible space.
     */
    public Map<String, String> getSpaces() {
        Hashtable h = new Hashtable();

        for (Space space : spaceManager.getAllSpaces()) {
            h.put(space.getKey(), space.getName());
        }

        return h;
    }

    /**
     * Get a list with available groups.
     */
    public List<String> getGroups() throws EntityException {
        List<String> result = new ArrayList<String>();

        for (Group group : groupManager.getGroups()) {
            result.add(group.getName());
        }

        return result;
    }
}
