package de.dm.mail2blog;

import de.dm.mail2blog.base.*;

import java.util.regex.Pattern;

/**
 * Upgrade the mail configuration of a previous version to be compatible with the current version.
 */
public class UpgradeMailConfiguration {
    public static void upgradeMailConfiguration(MailConfiguration mailConfiguration) {
        upgradeSpaceInSubjectOption(mailConfiguration);
        upgradeSpaceInAddressOption(mailConfiguration);
    }

    /**
     * The old spaceInAddress option has been replaced with SpaceRules in version 1.2.
     */
    private static void upgradeSpaceInAddressOption(MailConfiguration mailConfiguration) {
        if (mailConfiguration.getSpaceKeyInAddress()) {
            mailConfiguration.setSpaceKeyInAddress(false);

            String configMail = mailConfiguration.getEmailaddress();
            int index = configMail.indexOf('@');
            if (index > 0 && (index < configMail.length() -1)) {
                String prefix = configMail.substring(0, index);
                String suffix = configMail.substring(index);

                String pattern = "^" + Pattern.quote(prefix) + "\\+(.+)" + Pattern.quote(suffix) + "$";

                SpaceRule spaceInAddressRule = SpaceRule.builder()
                    .field(SpaceRuleFields.ToCC)
                    .operator(SpaceRuleOperators.Regexp)
                    .value(pattern)
                    .action(SpaceRuleActions.MOVE)
                    .space(SpaceRuleSpaces.CapturingGroup1)
                    .build();

                // Create a new set of spaces rules with the space in address rule and copy all existing space rules to it.
                SpaceRule[] oldSpaceRules = mailConfiguration.getSpaceRules();
                SpaceRule[] newSpaceRules = new SpaceRule[oldSpaceRules.length + 1];
                newSpaceRules[0] = spaceInAddressRule;
                System.arraycopy(mailConfiguration.getSpaceRules(), 0, newSpaceRules, 1, oldSpaceRules.length);

                mailConfiguration.setSpaceRules(newSpaceRules);
            };

        }
    }

    /**
     * The old spaceInSubject option has been replaced with SpaceRules in version 1.2.
     */
    private static void upgradeSpaceInSubjectOption(MailConfiguration mailConfiguration) {
        if (mailConfiguration.getSpaceKeyInSubject()) {
            mailConfiguration.setSpaceKeyInSubject(false);

            SpaceRule spaceInSubjectRule = SpaceRule.builder()
                .field(SpaceRuleFields.SUBJECT)
                .operator(SpaceRuleOperators.Regexp)
                .value("^(.+):")
                .action(SpaceRuleActions.MOVE)
                .space(SpaceRuleSpaces.CapturingGroup1)
                .build();

            // Create a new set of spaces rules with the space in subject rule and copy all existing space rules to it.
            SpaceRule[] oldSpaceRules = mailConfiguration.getSpaceRules();
            SpaceRule[] newSpaceRules = new SpaceRule[oldSpaceRules.length + 1];
            newSpaceRules[0] = spaceInSubjectRule;
            System.arraycopy(mailConfiguration.getSpaceRules(), 0, newSpaceRules, 1, oldSpaceRules.length);

            mailConfiguration.setSpaceRules(newSpaceRules);
        }
    }
}
