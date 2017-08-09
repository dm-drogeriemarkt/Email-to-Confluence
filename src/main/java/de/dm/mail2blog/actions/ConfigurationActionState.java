package de.dm.mail2blog.actions;

import de.dm.mail2blog.GlobalState;
import de.dm.mail2blog.MailConfigurationWrapper;
import lombok.*;

/**
 * Autowired bean that stores the mailConfiguration currently used in the settings form.
 */
public class ConfigurationActionState {
    // Autowired objects.
    @Setter GlobalState globalState;

    @Setter MailConfigurationWrapper mailConfigurationWrapper;

    /**
     * Get the mailConfiguration currently being edited or duplicate the actively used one.
     *
     * @return the mail configuration currently being edited
     */
    public MailConfigurationWrapper getMailConfigurationWrapper() {
        if (mailConfigurationWrapper == null) {
            mailConfigurationWrapper = globalState.getMailConfigurationWrapper().duplicate();
        }

        return mailConfigurationWrapper;
    }
}
