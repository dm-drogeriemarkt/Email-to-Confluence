package de.dm.mail2blog;

import java.util.*;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.Message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VERPSpaceKeyExtractor implements ISpaceKeyExtractor {

    /**
     * Get the space key from the recipient email address.
     * The space key is extracted in the form "email+spacekey@domain.net".
     *
     * @param mailConfigurationWrapper The config to use
     * @param message The mail message from which to extract the space key.
     *
     * @return Returns a space key
     */
    public List<String> getSpaceKeys(MailConfigurationWrapper mailConfigurationWrapper, Message message)
    {
        ArrayList<String> result = new ArrayList<String>();
        ArrayList<Address> recipients = new ArrayList<Address>();

        String configMail = mailConfigurationWrapper.getMailConfiguration().getEmailaddress();
        int index = configMail.indexOf('@');
        if (index > 0 && (index < configMail.length() -1)) {
            String prefix = configMail.substring(0, index);
            String suffix = configMail.substring(index);

            // Get the address in the To: header.
            try {
                Address[] recipientTo = message.getRecipients(Message.RecipientType.TO);
                if (recipientTo != null) {
                    recipients.addAll(Arrays.asList(recipientTo));
                }
            } catch (MessagingException e) {
                log.warn("Mail2Blog: MessagingExcpetion(" + e.getMessage() + ")");
            }


            // Get the address in the CC: header.
            try {
                Address[] recipientCC = message.getRecipients(Message.RecipientType.CC);
                if (recipientCC != null) {
                    recipients.addAll(Arrays.asList(recipientCC));
                }
            } catch (MessagingException e) {
                log.warn("Mail2Blog: MessagingExcpetion(" + e.getMessage() + ")");
            }

            // Loop through all addresses until we found one where we can extract a space key.
            for (Address recipient : recipients) {
                // Retrieve the email address.
                String emailAddress = (recipient instanceof InternetAddress)
                        ? ((InternetAddress) recipient).getAddress()
                        : recipient.toString();

                if (emailAddress.startsWith(prefix + "+") && emailAddress.endsWith(suffix)) {
                    String space = emailAddress.substring(
                            prefix.length() + 1,
                            emailAddress.length() - suffix.length()
                    );
                    result.add(space);
                }
            }
        }

        return result;
    }
}
