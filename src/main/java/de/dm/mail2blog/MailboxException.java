package de.dm.mail2blog;

public class MailboxException extends Exception {
    public MailboxException() {}
    public MailboxException(String message) { super(message); }
    public MailboxException(Throwable cause) { super(cause); }
    public MailboxException(String message, Throwable cause) { super(message, cause); }
}
