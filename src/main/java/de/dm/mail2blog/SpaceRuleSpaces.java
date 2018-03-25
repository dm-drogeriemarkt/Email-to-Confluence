package de.dm.mail2blog;

import com.atlassian.confluence.spaces.SpaceManager;

// Spaces that can be used in space rules.
public abstract class SpaceRuleSpaces {
    public static final String CapturingGroup0 = "_group_0";
    public static final String CapturingGroup1 = "_group_1";

    /**
     * Validate that a given string is a valid space.
     */
    public static boolean validate(String operator, SpaceManager spaceManager, String check) {
        if (operator.equals(SpaceRuleOperators.Regexp)) {
            if (SpaceRuleSpaces.CapturingGroup0.equals(check)) return true;
            if (SpaceRuleSpaces.CapturingGroup1.equals(check)) return true;
        }

        if (spaceManager.getSpace(check) != null) {
            return true;
        }
        return false;
    }
}
