package de.dm.mail2blog;

// Content types
public abstract class ContentTypes {
    public static final String Page = "page";
    public static final String BlogPost = "blog";

    /**
     * Validate that a given string is a valid content type.
     */
    public static boolean validate(String check) {
        if (Page.equals(check)) return true;
        if (BlogPost.equals(check)) return true;
        return false;
    }
}
