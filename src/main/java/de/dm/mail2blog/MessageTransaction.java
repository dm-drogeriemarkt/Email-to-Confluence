package de.dm.mail2blog;

import com.atlassian.sal.api.transaction.TransactionCallback;
import lombok.*;
import lombok.extern.log4j.Log4j;
import com.atlassian.confluence.spaces.Space;
import javax.mail.Message;
import java.util.*;

/**
 * Transaction that processes an email message and creates a Confluence blog post from it.
 * Called by Mail2BlogJob.
 */
@Log4j
@Builder
class MessageTransaction implements TransactionCallback<Void> {
    @NonNull private Message message;
    @NonNull private MailConfigurationWrapper mailConfigurationWrapper;
    @NonNull private Mailbox mailbox;

    public Void doInTransaction() {
        boolean status = true;

        try {
            // Get space
            List<Space> spaces = SpaceFactory.getSpace(mailConfigurationWrapper, message);

            if (spaces.isEmpty()) {
                log.error("Mail2Blog: Failed to process message. Failed to get a valid space.");
                status = false;
            } else {
                // Process message.
                MessageToBlogPostProcessor processor = new MessageToBlogPostProcessor(mailConfigurationWrapper);
                processor.process(spaces.get(0), message);
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
}
