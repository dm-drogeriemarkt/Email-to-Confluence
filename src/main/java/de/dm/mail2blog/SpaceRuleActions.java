package de.dm.mail2blog;

// Actions that can be used in space rules.
public abstract class SpaceRuleActions {
    public static final String COPY = "copy";
    public static final String MOVE = "move";

    /**
     * Validate that a given string is a valid space rule action.
     */
    public static boolean validate(String check) {
        if (SpaceRuleActions.COPY.equals(check)) return true;
        if (SpaceRuleActions.MOVE.equals(check)) return true;
        return false;
    }
}
