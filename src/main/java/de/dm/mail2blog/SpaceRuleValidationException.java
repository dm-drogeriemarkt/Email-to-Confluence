package de.dm.mail2blog;

public class SpaceRuleValidationException extends Exception {
    public SpaceRuleValidationException() {}
    public SpaceRuleValidationException(String message) { super(message); }
    public SpaceRuleValidationException(Throwable cause) { super(cause); }
    public SpaceRuleValidationException(String message, Throwable cause) { super(message, cause); }
}
