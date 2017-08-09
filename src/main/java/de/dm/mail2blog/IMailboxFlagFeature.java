package de.dm.mail2blog;

import javax.mail.Message;

public interface IMailboxFlagFeature {
    void flagAsProcessed(Message message) throws MailboxException;
    void flagAsInvalid(Message message) throws MailboxException;
}
