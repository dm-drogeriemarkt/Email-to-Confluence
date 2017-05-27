package de.dm.mail2blog;

public class MessageToBlogPostProcessorException extends Exception {

    public MessageToBlogPostProcessorException() {}
    public MessageToBlogPostProcessorException(String message) { super(message); }
    public MessageToBlogPostProcessorException(Throwable cause) { super(cause); }
    public MessageToBlogPostProcessorException(String message, Throwable cause) { super(message, cause); }
}
