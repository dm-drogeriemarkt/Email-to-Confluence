package de.dm.mail2blog;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * Get a html filter according to the mail configuration.
 */
public abstract class HtmlFilterFactory {

    /**
     * Build one htmlPolicyFactory from htmlFilters to filter HTML.
     *
     * @return policy factory one can use to filter html
     */
    public static PolicyFactory makeHtmlFilter(MailConfigurationWrapper mailConfigurationWrapper) {
        PolicyFactory result = new HtmlPolicyBuilder().toFactory();
        if (mailConfigurationWrapper.getMailConfiguration().getHtmlFilterFormatting()) { result = result.and(HtmlSanitizers.FORMATTING); }
        if (mailConfigurationWrapper.getMailConfiguration().getHtmlFilterBlocks()) { result = result.and(HtmlSanitizers.BLOCKS); }
        if (mailConfigurationWrapper.getMailConfiguration().getHtmlFilterImages()) { result = result.and(HtmlSanitizers.IMAGES); }
        if (mailConfigurationWrapper.getMailConfiguration().getHtmlFilterLinks()) { result = result.and(HtmlSanitizers.LINKS); }
        if (mailConfigurationWrapper.getMailConfiguration().getHtmlFilterStyles()) { result = result.and(HtmlSanitizers.STYLES); }
        if (mailConfigurationWrapper.getMailConfiguration().getHtmlFilterTables()) { result = result.and(HtmlSanitizers.TABLES); }
        return result;
    }
}
