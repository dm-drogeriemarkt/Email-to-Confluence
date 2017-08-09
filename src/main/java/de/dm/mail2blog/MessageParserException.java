package de.dm.mail2blog;

public class MessageParserException extends Exception {

    public MessageParserException() {}
    public MessageParserException(String message) { super(message); }
    public MessageParserException(Throwable cause) { super(cause); }
    public MessageParserException(String message, Throwable cause) { super(message, cause); }
}
