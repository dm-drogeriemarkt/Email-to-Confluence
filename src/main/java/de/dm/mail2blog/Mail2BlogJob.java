package de.dm.mail2blog;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import lombok.Cleanup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.Message;

/**
 * The job triggered by confluence that polls for new messages and creates pages/blog posts.
 */
@Slf4j
@Component
@ExportAsService
public class Mail2BlogJob implements JobRunner
{
    // Auto wired components.
    @Setter @Autowired          private GlobalState globalState;
    @Setter @Autowired          private SpaceExtractor spaceExtractor;

    /**
     * The main method of this job.
     * Called by confluence every time the mail2blog trigger fires.
     */
    public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
        log.info("Mail2Blog: executing job");

        if ("true".equals(systemGetProperty("atlassian.mail.fetchdisabled"))) {
            return JobRunnerResponse.aborted("Aborting because of atlassian.mail.fetchdisabled=true.");
        }

        if ("true".equals(systemGetProperty("atlassian.mail.popdisabled"))) {
            return JobRunnerResponse.aborted("Aborting because of atlassian.mail.popdisabled=true.");
        }

        try {
            MailConfigurationWrapper mailConfigurationWrapper = globalState.getMailConfigurationWrapper();
            @Cleanup Mailbox mailbox = newMailbox(mailConfigurationWrapper);

            // Go through all messages in the INBOX.
            // Processed messages get deleted or moved into different folders.
            // Go through messages in reverse order to prevent messing up the
            // index when deleting messages from the top and to post the newest Mail as newest blog post.
            Message[] messages = mailbox.getMessages();
            for (int i = messages.length -1; i >= 0; i--) {
                Message message = messages[i];

                // Process message.
                MessageTransaction transaction = MessageTransaction.builder()
                .spaceExtractor(spaceExtractor)
                .mailConfigurationWrapper(mailConfigurationWrapper)
                .mailbox(mailbox)
                .message(message)
                .build();
                getTransactionTemplate().execute(transaction);
            }
        } catch (Throwable e) {
            log.error("Mail2Blog: " + e.toString(), e);
            return JobRunnerResponse.failed(e);
        }

        return JobRunnerResponse.success();
    }

    public TransactionTemplate getTransactionTemplate() {
        return StaticAccessor.getTransactionTemplate();
    }

    public Mailbox newMailbox(MailConfigurationWrapper mailConfigurationWrapper) throws MailboxException{
        return new Mailbox(mailConfigurationWrapper);
    }

    public String systemGetProperty(String key) {
        return System.getProperty(key);
    }
}
