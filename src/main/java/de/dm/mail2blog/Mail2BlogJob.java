package de.dm.mail2blog;

import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.mail.Message;

/**
 * The job triggered by confluence that polls for new messages
 * and creates blog posts.
 */
@Slf4j
public class Mail2BlogJob implements JobRunner, IMail2BlogJob
{
    // Auto wired components.
    @Setter TransactionTemplate transactionTemplate;
    @Setter GlobalState globalState;

    /**
     * The main method of this job.
     * Called by confluence every time the mail2blog trigger fires.
     */
    public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
        log.info("Mail2Blog: Executing job");

        if ("true".equals(System.getProperty("atlassian.mail.fetchdisabled"))) {
            return JobRunnerResponse.aborted("Aborting because of atlassian.mail.fetchdisabled=true.");
        }

        if ("true".equals(System.getProperty("atlassian.mail.popdisabled"))) {
            return JobRunnerResponse.aborted("Aborting because of atlassian.mail.popdisabled=true.");
        }

        try {
            MailConfigurationWrapper mailConfigurationWrapper = globalState.getMailConfigurationWrapper();
            @Cleanup Mailbox mailbox = new Mailbox(mailConfigurationWrapper);

            // Go through all messages in the INBOX.
            // Processed messages get deleted or moved into different folders.
            // Go through messages in reverse order to prevent messing up the
            // index when deleting messages from the top and to post the newest Mail as newest blog post.
            Message[] messages = mailbox.getMessages();
            for (int i = messages.length -1; i >= 0; i--) {
                Message message = messages[i];

                // Process message.
                MessageTransaction transaction = MessageTransaction.builder()
                .mailConfigurationWrapper(mailConfigurationWrapper)
                .mailbox(mailbox)
                .message(message)
                .build();
                transactionTemplate.execute(transaction);
            }
        } catch (Exception e) {
            log.error("Mail2Blog: " + e.getMessage(), e);
            return JobRunnerResponse.failed(e);
        }

        return JobRunnerResponse.success();
    }
}
