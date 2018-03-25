package de.dm.mail2blog;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Autowired bean that stores the plugin wide state.
 */
@Component
@ExportAsService
public class GlobalState {

    @Setter @Autowired private MailConfigurationManager mailConfigurationManager;

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
