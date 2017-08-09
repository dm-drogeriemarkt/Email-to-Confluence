package de.dm.mail2blog;

import lombok.extern.slf4j.Slf4j;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SubjectSpaceKeyExtractor implements ISpaceKeyExtractor {

    /**
     * Get the space key from the subject line.
     * The space key is extracted in the form "spacekey: Subject".
     *
     * @param mailConfigurationWrapper the config to use
     * @param message The mail message from which to extract the space key.
     *
     * @return A space key or an empty list
     */
    public List<String> getSpaceKeys(MailConfigurationWrapper mailConfigurationWrapper, Message message)
    {
        ArrayList<String> result = new ArrayList<String>();

        String subject = null;
        try {
            subject = message.getSubject();
            int index = subject.indexOf(':');
            if (index > 0) {
                result.add(subject.substring(0, index).trim());
            }
        } catch (MessagingException e) {
            log.debug("Mail2Blog: Could not get subject from message.");
        }

        return result;
    }
}
