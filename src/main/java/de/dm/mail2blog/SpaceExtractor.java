package de.dm.mail2blog;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SpaceExtractor {
    /**
     * @param mailConfigurationWrapper The config to use
     * @param message The mail message from which to extract the space key.
     *
     * @return Returns a list of space keys
     */
    public List<SpaceInfo> getSpaces(MailConfigurationWrapper mailConfigurationWrapper, Message message)
    {
        // Evaluate space rules.
        ArrayList<SpaceInfo> spaces = new ArrayList<SpaceInfo>();
        HashMap<String, Void> seenSpaceKeys = new HashMap<String, Void>();
        for (SpaceRule rule : mailConfigurationWrapper.getMailConfiguration().getSpaceRules()) {
            boolean ruleMatched = false;

            try {
                List<String > values = extractValues(rule, message);
                for (String value: values) {
                    if (evalCondition(rule, value)) {
                        ruleMatched = true;

                        String spaceKey = extractSpaceKey(rule, value);

                        if (!seenSpaceKeys.containsKey(spaceKey)) {
                            seenSpaceKeys.put(spaceKey, null);

                            // Get space
                            Space space = getSpaceManager().getSpace(spaceKey);
                            if (space == null) {
                                log.warn("Mai2Blog: invalid space key " + spaceKey);
                                continue;
                            }

                            // Add space.
                            spaces.add(SpaceInfo.builder().space(space).contentType(rule.getContentType()).build());
                        }
                    }
                }
            } catch (Exception e) {
                String info = "";
                try {
                    info = new ObjectMapper().writeValueAsString(rule);
                    info = " for SpaceRule" + info;
                } catch (Exception e2) {}

                log.warn("Mail2Blog: (" + e.toString() + ")" + info, e);
            }

            // A move rule is always the finial rule that gets applied.
            if (ruleMatched && SpaceRuleActions.MOVE.equals(rule.getAction())) {
                return spaces;
            }
        }

        // Add default space to spaceKeys.
        Space defaultSpace = getSpaceManager().getSpace(mailConfigurationWrapper.getMailConfiguration().getDefaultSpace());
        if (defaultSpace == null) {
            log.warn("Mail2Blog: Invalid default space");
        } else {
            spaces.add(SpaceInfo.builder()
                .space(defaultSpace)
                .contentType(mailConfigurationWrapper.getMailConfiguration().getDefaultContentType())
                .build()
            );
        }

        return spaces;
    }

    /**
     * Get one ore multiple possible values from the given message according to the field specified in the SpaceRule.
     */
    private List<String> extractValues(SpaceRule rule, Message message) throws MessagingException {
        ArrayList<String> values = new ArrayList<String>();

        if (SpaceRuleFields.FROM.equals(rule.getField())) {
            Address[] from = message.getFrom();
            if (from != null) {
                for (Address a : from) {
                    String emailAddress = (a instanceof InternetAddress)
                            ? ((InternetAddress) a).getAddress()
                            : a.toString();
                    values.add(emailAddress.trim());
                }
            }
        }

        if (SpaceRuleFields.TO.equals(rule.getField()) || SpaceRuleFields.ToCC.equals(rule.getField())) {
            Address[] to = message.getRecipients(Message.RecipientType.TO);
            if (to != null) {
                for (Address a : to) {
                    String emailAddress = (a instanceof InternetAddress)
                            ? ((InternetAddress) a).getAddress()
                            : a.toString();
                    values.add(emailAddress.trim());
                }
            }
        }

        if (SpaceRuleFields.CC.equals(rule.getField()) || SpaceRuleFields.ToCC.equals(rule.getField())) {
            Address[] cc = message.getRecipients(Message.RecipientType.CC);
            if (cc != null) {
                for (Address a : cc) {
                    String emailAddress = (a instanceof InternetAddress)
                            ? ((InternetAddress) a).getAddress()
                            : a.toString();
                    values.add(emailAddress.trim());
                }
            }
        }

        if (SpaceRuleFields.SUBJECT.equals(rule.getField())) {
            values.add(message.getSubject().trim());
        }

        return values;
    }

    /**
     * Check if given value fulfills the condition given in the SpaceRule.
     *
     * @param rule
     *  The rule to evaluate.
     *
     * @param value
     *  The value extracted from a field.
     *
     * @return
     *  True if the condition is fulfilled, false if not.
     */
    private boolean evalCondition(SpaceRule rule, String value) {
        if (SpaceRuleOperators.Is.equals(rule.getOperator())) {
            return StringUtils.equalsIgnoreCase(value, rule.getValue());
        } else if (SpaceRuleOperators.Contains.equals(rule.getOperator())) {
            return StringUtils.containsIgnoreCase(value, rule.getValue());
        } else if (SpaceRuleOperators.StartsWith.equals(rule.getOperator())) {
            return StringUtils.startsWithIgnoreCase(value, rule.getValue());
        } else if (SpaceRuleOperators.EndsWith.equals(rule.getOperator())) {
            return StringUtils.endsWithIgnoreCase(value, rule.getValue());
        } else if (SpaceRuleOperators.Regexp.equals(rule.getOperator())) {
            Pattern pattern = Pattern.compile(rule.getValue(), Pattern.CASE_INSENSITIVE);
            return pattern.matcher(value).find();
        }

        return false;
    }

    /**
     * Get the space key for a rule.
     *
     * Usually this is just rule.space, but for regexps the space key can be extracted from value.
     */
    private String extractSpaceKey(SpaceRule rule, String value) throws Exception {
        // Extract space key with regexp.
        if (
            SpaceRuleOperators.Regexp.equals(rule.getOperator()) &&
            (SpaceRuleSpaces.CapturingGroup0.equals(rule.getSpace()) || SpaceRuleSpaces.CapturingGroup1.equals(rule.getSpace()))
        ) {
            Pattern pattern = Pattern.compile(rule.getValue(), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(value);

            if (!matcher.find()) {
                throw new Exception("regexp did not match");
            }

            if (SpaceRuleSpaces.CapturingGroup0.equals(rule.getSpace())) {
                return matcher.group(0);
            } else {
                if (matcher.groupCount() < 1) {
                    throw new Exception("no capturing group 1");
                }
                return matcher.group(1);
            }
        }

        return rule.getSpace();
    }

    public SpaceManager getSpaceManager() {
        return StaticAccessor.getSpaceManager();
    }
}
