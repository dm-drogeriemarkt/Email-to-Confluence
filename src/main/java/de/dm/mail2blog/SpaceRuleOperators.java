package de.dm.mail2blog;

// Operators that can be used in space rules
public abstract class SpaceRuleOperators {
    public static final String Is = "is";
    public static final String StartsWith = "start";
    public static final String EndsWith = "end";
    public static final String Contains = "contains";
    public static final String Regexp = "regexp";
    public static final String RegexpMatchgroup = "regexpMatchgroup";

    /**
     * Validate that a given string is a valid space rule field.
     */
    public static boolean validate(String check) {
        if (SpaceRuleOperators.Is.equals(check)) return true;
        if (SpaceRuleOperators.StartsWith.equals(check)) return true;
        if (SpaceRuleOperators.EndsWith.equals(check)) return true;
        if (SpaceRuleOperators.Contains.equals(check)) return true;
        if (SpaceRuleOperators.Regexp.equals(check)) return true;
        if (SpaceRuleOperators.RegexpMatchgroup.equals(check)) return true;
        return false;
    }
}
