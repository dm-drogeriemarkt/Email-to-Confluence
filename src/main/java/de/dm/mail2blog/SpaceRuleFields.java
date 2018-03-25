package de.dm.mail2blog;

// Fields that can be used in space rules.
public abstract class SpaceRuleFields {
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String CC = "cc";
    public static final String ToCC = "to/cc"; // TO header or CC
    public static final String SUBJECT = "subject";

    /**
     * Validate that a given string is a valid space rule field.
     */
    public static boolean validate(String check) {
        if (SpaceRuleFields.FROM.equals(check)) return true;
        if (SpaceRuleFields.TO.equals(check)) return true;
        if (SpaceRuleFields.CC.equals(check)) return true;
        if (SpaceRuleFields.ToCC.equals(check)) return true;
        if (SpaceRuleFields.SUBJECT.equals(check)) return true;
        return false;
    }
}
