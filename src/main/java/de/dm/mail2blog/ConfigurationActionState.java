package de.dm.mail2blog;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Autowired bean that stores the mailConfiguration currently used in the settings form.
 */
@Component
public class ConfigurationActionState {
    // Autowired objects.
    @Setter @Autowired GlobalState globalState;

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
