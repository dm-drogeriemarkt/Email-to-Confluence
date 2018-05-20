package de.dm.mail2blog;

public class MessageToContentProcessorException extends Exception {

    public MessageToContentProcessorException() {}
    public MessageToContentProcessorException(String message) { super(message); }
    public MessageToContentProcessorException(Throwable cause) { super(cause); }
    public MessageToContentProcessorException(String message, Throwable cause) { super(message, cause); }
}
