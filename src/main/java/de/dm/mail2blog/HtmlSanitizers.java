package de.dm.mail2blog;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * Predefined PolicyFactories to use with the OWASP html filter framework.
 */
public final class HtmlSanitizers {
    // Use the predefined filters from the OWASP framework.
    public static final PolicyFactory FORMATTING = Sanitizers.FORMATTING;
    public static final PolicyFactory BLOCKS     = Sanitizers.BLOCKS;
    public static final PolicyFactory STYLES     = Sanitizers.STYLES;
    public static final PolicyFactory LINKS      = Sanitizers.LINKS;
    public static final PolicyFactory IMAGES     = Sanitizers.IMAGES;

    // Extend the predefined TABLES filter to allow outdated formatting attributes
    // that are still commonly used in HTML mails.
    public static final PolicyFactory TABLES = Sanitizers.TABLES.and(
        new HtmlPolicyBuilder()
        .allowElements(new String[]{
            "table",
            "tr",
            "td",
            "th",
            "colgroup",
            "col",
            "thead",
            "tbody",
            "tfoot"
        })
        .allowAttributes(new String[]{
            "width",
            "cellspacing",
            "cellpadding",
            "border",
        }).onElements(new String[] {"table"})
        .allowAttributes(new String[]{
            "scope",
            "nowrap",
            "rowspan",
            "colspan",
            "width",
            "height",
        }).onElements(new String[] {"td", "th"})
        .allowAttributes(new String[]{
            "bgcolor",
            "char",
            "charoff",
        }).onElements(new String[]{
            "table",
            "tr",
            "td",
            "th",
            "colgroup",
            "col",
            "thead",
            "tbody",
            "tfoot"
        })
        .toFactory()
    );
}
