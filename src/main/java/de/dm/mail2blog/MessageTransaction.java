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
 * Transaction that processes an email message and creates Confluence pages/blog posts from it.
 * Called by Mail2BlogJob.
 */
@Log4j
@Builder
public class MessageTransaction implements TransactionCallback<Void> {
    @Getter @NonNull private Message message;
    @Getter @NonNull private MailConfigurationWrapper mailConfigurationWrapper;
    @Getter @NonNull private Mailbox mailbox;
    @Getter @NonNull @Autowired private SpaceExtractor spaceExtractor;

    public Void doInTransaction() {
        boolean status = true;

        try {
            // Get space
            List<SpaceInfo> spaceInfos = spaceExtractor.getSpaces(mailConfigurationWrapper, message);

            if (spaceInfos.isEmpty()) {
                log.error("Mail2Blog: failed to process message. Failed to get a valid space.");
                status = false;
            } else {
                for (SpaceInfo spaceInfo : spaceInfos) {
                    // Process message.
                    MessageToContentProcessor processor = newMessageToBlogProcessor(mailConfigurationWrapper);
                    processor.process(spaceInfo.getSpace(), message, spaceInfo.getContentType());
                }
            }
        } catch (Exception e) {
            status = false;
            log.error("Mail2Blog: failed to process message", e);
        }

        try {
            if (status) {
                mailbox.flagAsProcessed(message);
            } else {
                mailbox.flagAsInvalid(message);
            }
        } catch (Exception e) {
            log.error("Mail2Blog: failed to flag message", e);
        }

        return null;
    }

    public MessageToContentProcessor newMessageToBlogProcessor(MailConfigurationWrapper mailConfigurationWrapper)
    throws MailConfigurationManagerException {
        MessageToContentProcessor processor = new MessageToContentProcessor(mailConfigurationWrapper);
        ContainerManager.autowireComponent(processor);
        return processor;
    }
}
