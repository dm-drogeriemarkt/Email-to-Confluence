package de.dm.mail2blog;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.spring.container.ContainerManager;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.Message;
import java.util.List;

/**
 * Transaction that processes an email message and creates a Confluence blog post from it.
 * Called by Mail2BlogJob.
 */
@Log4j
@Builder
public class MessageTransaction implements TransactionCallback<Void> {
    @Getter @NonNull private Message message;
    @Getter @NonNull private MailConfigurationWrapper mailConfigurationWrapper;
    @Getter @NonNull private Mailbox mailbox;
    @Getter @NonNull @Autowired private SpaceKeyExtractor spaceKeyExtractor;

    public Void doInTransaction() {
        boolean status = true;

        try {
            // Get space
            List<Space> spaces = spaceKeyExtractor.getSpaces(mailConfigurationWrapper, message);

            if (spaces.isEmpty()) {
                log.error("Mail2Blog: Failed to process message. Failed to get a valid space.");
                status = false;
            } else {
                for (Space space : spaces) {
                    // Process message.
                    MessageToBlogPostProcessor processor = newMessageToBlogProcessor(mailConfigurationWrapper);
                    processor.process(space, message);
                }
            }
        } catch (Exception e) {
            status = false;
            log.error("Mail2Blog: Failed to process message", e);
        }

        try {
            if (status) {
                mailbox.flagAsProcessed(message);
            } else {
                mailbox.flagAsInvalid(message);
            }
        } catch (Exception e) {
            log.error("Mail2Blog: Failed to flag message", e);
        }

        return null;
    }

    public MessageToBlogPostProcessor newMessageToBlogProcessor(MailConfigurationWrapper mailConfigurationWrapper)
    throws MailConfigurationManagerException {
        MessageToBlogPostProcessor processor = new MessageToBlogPostProcessor(mailConfigurationWrapper);
        ContainerManager.autowireComponent(processor);
        return processor;
    }
}
