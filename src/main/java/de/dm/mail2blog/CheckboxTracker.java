package de.dm.mail2blog;

import com.atlassian.xwork.ParameterSafe;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * This class is a workaround for the issue that checkboxes only transmit
 * a true value, but not a false one. This class remembers when a setter with true is called.
 * When calling commit all checkboxes that have been set will be set to true all others to false.
 */
@ParameterSafe // https://developer.atlassian.com/confdev/confluence-plugin-guide/confluence-plugin-module-types/xwork-webwork-module/xwork-plugin-complex-parameters-and-security
@Component
public class CheckboxTracker {
    @Setter boolean secure = false;
    @Setter boolean checkCertificates = false;
    @Setter boolean htmlFilterBlocks = false;
    @Setter boolean htmlFilterFormatting = false;
    @Setter boolean htmlFilterImages = false;
    @Setter boolean htmlFilterLinks = false;
    @Setter boolean htmlFilterStyles = false;
    @Setter boolean htmlFilterTables = false;
    @Setter boolean gallerymacro = false;
    @Setter boolean htmlmacro = false;
    @Setter boolean spaceKeyInAddress = false;
    @Setter boolean spaceKeyInSubject = false;
    @Setter boolean doNotShowPop3Confirmation = false;

    /**
     * Reset all checkbox values to false.
     */
    public void reset() {
        secure = false;
        checkCertificates = false;
        htmlFilterBlocks = false;
        htmlFilterFormatting = false;
        htmlFilterImages = false;
        htmlFilterLinks = false;
        htmlFilterStyles = false;
        htmlFilterTables = false;
        gallerymacro = false;
        htmlmacro = false;
        spaceKeyInAddress = false;
        spaceKeyInSubject = false;
    }

    /**
     * Set checkbox values on mail configuration.
     */
    public void commit(MailConfiguration mailConfiguration) {
        mailConfiguration.setSecure(secure);
        mailConfiguration.setCheckCertificates(checkCertificates);
        mailConfiguration.setHtmlFilterBlocks(htmlFilterBlocks);
        mailConfiguration.setHtmlFilterFormatting(htmlFilterFormatting);
        mailConfiguration.setHtmlFilterImages(htmlFilterImages);
        mailConfiguration.setHtmlFilterLinks(htmlFilterLinks);
        mailConfiguration.setHtmlFilterStyles(htmlFilterStyles);
        mailConfiguration.setHtmlFilterTables(htmlFilterTables);
        mailConfiguration.setGallerymacro(gallerymacro);
        mailConfiguration.setHtmlmacro(htmlmacro);
        mailConfiguration.setSpaceKeyInAddress(spaceKeyInAddress);
        mailConfiguration.setSpaceKeyInSubject(spaceKeyInSubject);
        mailConfiguration.setDoNotShowPop3Confirmation(doNotShowPop3Confirmation);
    }
}
