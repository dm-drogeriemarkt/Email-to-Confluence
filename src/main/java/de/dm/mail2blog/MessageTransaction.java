package de.dm.mail2blog;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.spring.container.ContainerManager;
import de.dm.mail2blog.base.SpaceExtractor;
import de.dm.mail2blog.base.SpaceInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.Message;
import java.util.List;

/**
 * Transaction that processes an email message and creates Confluence pages/blog posts from it.
 * Called by Mail2BlogJob.
 */
@Slf4j
@Builder
public class MessageTransaction implements TransactionCallback<Void> {
    @Getter private Message message;
    @Getter private MailConfigurationWrapper mailConfigurationWrapper;
    @Getter private Mailbox mailbox;
    @Getter private SpaceExtractor spaceExtractor;
    @Getter private SpaceManager spaceManager;

    public Void doInTransaction() {
        boolean status = true;

        try {
            // Get space
            List<SpaceInfo> spaceInfos = spaceExtractor.getSpaces(mailConfigurationWrapper.getMail2BlogBaseConfiguration(), message);

            if (spaceInfos.isEmpty()) {
                log.error("Mail2Blog: failed to process message. Failed to get a valid space.");
                status = false;
            } else {
                for (SpaceInfo spaceInfo : spaceInfos) {
                    // Process message.
                    MessageToContentProcessor processor = newMessageToBlogProcessor(mailConfigurationWrapper);
                    Space space = spaceManager.getSpace(spaceInfo.getSpaceKey());
                    if (space == null) {
                        log.error("Mail2Blog: invalid space in SpaceInfo");
                    }
                    processor.process(space, message, spaceInfo.getContentType());
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
