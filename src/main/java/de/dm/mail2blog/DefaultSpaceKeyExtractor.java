package de.dm.mail2blog;

import javax.mail.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Get the space key set as default in the settings.
 */
public class DefaultSpaceKeyExtractor implements ISpaceKeyExtractor {

    /**
     * Get the default space key.
     */
    public List<String> getSpaceKeys(MailConfigurationWrapper mailConfigurationWrapper, Message message) {
        ArrayList<String> result = new ArrayList<String>();
        result.add(mailConfigurationWrapper.getMailConfiguration().getDefaultSpace());
        return result;
    }
}
