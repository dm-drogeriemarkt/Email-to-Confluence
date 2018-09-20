package de.dm.mail2blog;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Strategy to flag messages as processed and invalid on POP3 mailboxes.
 */
@RequiredArgsConstructor
public class Pop3MailboxFlagStrategy implements IMailboxFlagFeature {
    @NonNull private Mailbox mailbox;

    /**
     * Flag message to be deleted.
     */
    private void deleteMessage(Message message) throws MailboxException {
        try {
            message.setFlag(Flags.Flag.DELETED, true);
        } catch (MessagingException e) {
            throw new MailboxException("failed to mark message to be deleted", e);
        }
    }

    /**
     * On POP3 since we can't move messages around,
     * just flag them to be deleted.
     */
    public void flagAsProcessed(Message message) throws MailboxException {
        this.deleteMessage(message);
    }

    /**
     * On POP3 since we can't move messages around,
     * just flag them to be deleted.
     */
    public void flagAsInvalid(Message message) throws MailboxException {
        this.deleteMessage(message);
    }
}
