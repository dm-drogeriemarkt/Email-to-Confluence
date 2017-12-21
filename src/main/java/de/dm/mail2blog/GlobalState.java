package de.dm.mail2blog;

import lombok.*;

/**
 * Autowired bean that stores the plugin wide state.
 */
public class GlobalState implements IGlobalState {

    // Auto wired components.
    @Setter private MailConfigurationManager mailConfigurationManager;

    /**
     * The currently used configuration
     */
     @Setter MailConfigurationWrapper mailConfigurationWrapper = null;

    /**
     * Get the mail configuration or lazy load the current active configuration from storage.
     *
     * @return The mail configuration currently used for processing
     */
    public MailConfigurationWrapper getMailConfigurationWrapper() {
        if (mailConfigurationWrapper == null) {
            mailConfigurationWrapper = new MailConfigurationWrapper(
                mailConfigurationManager.loadConfig()
            );
        }

        return mailConfigurationWrapper;
    }
}
