package de.dm.mail2blog;

import javax.mail.Message;
import java.util.List;

public interface ISpaceKeyExtractor {

    /**
     * Extract the space key from a message.
     *
     * @param mailConfigurationWrapper
     *  The mail configuration that has been used to access the mailbox.
     *
     * @param message
     *  A email for which a space key should be found.
     *
     * @return
     */
    public List<String> getSpaceKeys(MailConfigurationWrapper mailConfigurationWrapper, Message message);
}
