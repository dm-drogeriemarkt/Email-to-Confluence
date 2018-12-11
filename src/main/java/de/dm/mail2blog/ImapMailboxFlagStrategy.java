package de.dm.mail2blog;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.mail.*;

/**
 * Strategy to mark messages as processed/invalid on IMAP mailboxes.
 */
@RequiredArgsConstructor
@Slf4j
public class ImapMailboxFlagStrategy implements IMailboxFlagFeature
{
    @NonNull private Mailbox mailbox;

    /**
     * Get/create a folder below the inbox or the root folder.
     *
     * @param name The name of the folder to create.
     * @return The created folder
     * @throws MailboxException if communication with mailbox does not work as expected
     */
    private Folder getOrCreateFolder(@NonNull String name) throws MailboxException {
        Folder folder;
        try {
            folder = getOrCreateSubfolder(mailbox.getInbox(), name);
        } catch (MailboxException inboxError) {
            try {
                log.info("failed to create folder '" + name + "' below INBOX, falling back to ROOT folder", inboxError);
                folder = getOrCreateSubfolder(mailbox.getDefaultFolder(), name);
            } catch (MailboxException rootError) {
                throw new MailboxException("failed to get or create folder '" + name + '"', rootError);
            }
        }

        return folder;
    }

    private Folder getOrCreateSubfolder(Folder parent, @NonNull String name) throws MailboxException {
        Folder folder;

        try {
            folder = parent.getFolder(name);
        } catch (MessagingException me) {
            throw new MailboxException("failed to get folder '" + name + "'", me);
        }

        boolean folder_exists = false;
        try {
            folder_exists = folder.exists();
        } catch (MessagingException me) {
            throw new MailboxException("failed to check if folder '" + name + "' exists", me);

        }

        // Create folder if it does not exist.
        if (!folder_exists) {
            try {
                if (!folder.create(Folder.HOLDS_MESSAGES)) {
                    throw new MailboxException("failed to create '" + name + "' folder");
                }
            } catch (MessagingException me) {
                throw new MailboxException("failed to create '" + name + "' folder", me);
            }
        }

        // Open folder read write.
        try {
            folder.open(Folder.READ_WRITE);
        } catch (FolderNotFoundException fnfe)
        {
            throw new MailboxException("could not find folder '" + name + "'", fnfe);
        } catch (MessagingException e) {
            throw new MailboxException("could not open folder '" + name + "'", e);
        }

        return folder;
    }

    /**
     * Move a given message from one IMAP folder to another.
     *
     * @param m The message to be moved.
     * @param to The folder to where to move the message.
     */
    private void moveMessage(Message m, Folder to) throws MailboxException
    {
        try {
            // Copy the message to the destination folder.
            m.getFolder().copyMessages(new Message[] {m}, to);

            // Mark message as deleted and call expunge on folder.
            m.setFlag(Flags.Flag.DELETED, true);
            m.getFolder().expunge();
        }
        catch (MessagingException e)
        {
            throw new MailboxException("failed to move message", e);
        }
    }

    /**
     * Move message to processed folder.
     *
     * @param message the message to handle
     */
    public void flagAsProcessed(Message message) throws MailboxException {
        Folder to = this.getOrCreateFolder("Processed");
        this.moveMessage(message, to);
        try {
            to.close(true);
        } catch (MessagingException me) {
            throw new MailboxException("failed to close folder", me);
        }
    }

    /**
     * Move message to invalid folder.
     *
     * @param message the message to handle
     */
    public void flagAsInvalid(Message message) throws MailboxException {
        Folder to = this.getOrCreateFolder("Invalid");
        this.moveMessage(message, to);
        try {
            to.close(true);
        } catch (MessagingException me) {
            throw new MailboxException("failed to close folder", me);
        }
    }
}
