package de.dm.mail2blog;

import org.apache.commons.lang3.StringUtils;

import javax.mail.*;
import java.util.Properties;

/**
 * Read messages from mailbox and move messages around in folders.
 */
public class Mailbox implements IMailboxFlagFeature {
    /**
     * The configuration used to access the mailbox.
     */
    private MailConfigurationWrapper mailConfigurationWrapper;

    /**
     * The strategy to use to flag messages (depends on the protocol used).
     */
    private IMailboxFlagFeature flagStrategy;

    /**
     * The INBOX Folder.
     */
    private Folder inbox;

    /**
     * The default Folder (/).
     */
    private Folder defaultFolder;

    /**
     * The Mailstore to use.
     */
    private Store store;

    /**
     * Create a new Mailbox
     */
    public Mailbox(MailConfigurationWrapper mailConfigurationWrapper) {
        this.mailConfigurationWrapper = mailConfigurationWrapper;
    }

    private IMailboxFlagFeature getFlagStrategy() throws MailboxException {
        if (this.flagStrategy == null) {
            // Choose flag strategy based on protocol.
            if (mailConfigurationWrapper.getMailConfiguration().getProtocol().endsWith("pop3")) {
                this.flagStrategy = new Pop3MailboxFlagStrategy(this);
            } else if (mailConfigurationWrapper.getMailConfiguration().getProtocol().endsWith("imap")) {
                this.flagStrategy = new ImapMailboxFlagStrategy(this);
            } else {
                throw new MailboxException("unsupported protocol " + mailConfigurationWrapper.getMailConfiguration().getProtocol());
            }
        }
        return this.flagStrategy;
    }

    /**
     * Connect to the mail server and log in.
     */
    public Store getStore() throws MailboxException {
        if (this.store == null) {
            // sanity check.
            if (
                    StringUtils.isBlank(mailConfigurationWrapper.getMailConfiguration().getServer())
                            || StringUtils.isBlank(mailConfigurationWrapper.getMailConfiguration().getUsername())
                            || mailConfigurationWrapper.getMailConfiguration().getPassword() == null
            ) {
                throw new MailboxException("incomplete mail configuration settings (at least one setting is null/empty)");
            }

            // Create the properties for the session.
            Properties prop = new Properties();

            // Get the protocol to use.
            String protocol = mailConfigurationWrapper.getMailConfiguration().getProtocol();

            // Append s to protocol if we use the secure version.
            if (mailConfigurationWrapper.getMailConfiguration().getSecure()) {
                protocol += "s";
            }

            // Assemble the property prefix for this protocol.
            String propertyPrefix = "mail." + protocol;

            // Get the server port from the configuration and add it to the properties,
            // but only if it is actually set. If port = 0 this means we use the standard
            // port for the chosen protocol.
            if (mailConfigurationWrapper.getMailConfiguration().getPort() != 0) {
                prop.setProperty(propertyPrefix + ".port", "" + mailConfigurationWrapper.getMailConfiguration().getPort());
            }

            if (
                    mailConfigurationWrapper.getMailConfiguration().getSecure()
                            && mailConfigurationWrapper.getMailConfiguration().getCheckCertificates()
            ) {
                prop.setProperty(propertyPrefix + ".ssl.checkserveridentity", "true");
            } else {
                prop.setProperty(propertyPrefix + ".ssl.trust", "*");
                prop.setProperty(propertyPrefix + ".ssl.checkserveridentity", "false");
            }

            if (!StringUtils.isBlank(mailConfigurationWrapper.getMailConfiguration().getSslVersions())) {
                prop.setProperty(propertyPrefix + ".ssl.protocols", mailConfigurationWrapper.getMailConfiguration().getSslVersions());
            }

            // Set connection timeout (10 seconds).
            prop.setProperty(propertyPrefix + ".connectiontimeout", "10000");

            // Get the session for connecting to the mail server.
            Session session = getSessionInstance(prop, null);

            // Get the mail store, using the desired protocol.
            try {
                this.store = session.getStore(protocol);
            } catch (NoSuchProviderException e) {
                throw new MailboxException("no javax.mail provider for protocol " + protocol + ", please change the protocol", e);
            }

            // Connect to the mailstore.
            try {
                this.store.connect(
                        mailConfigurationWrapper.getMailConfiguration().getServer(),
                        mailConfigurationWrapper.getMailConfiguration().getUsername(),
                        mailConfigurationWrapper.getMailConfiguration().getPassword()
                );
            } catch (AuthenticationFailedException afe) {
                throw new MailboxException("authentication for mail store failed", afe);
            } catch (MessagingException me) {
                throw new MailboxException("connecting to mail store failed", me);
            } catch (IllegalStateException ise) {
                throw new MailboxException("connecting to mail store failed", ise);
            } catch (Exception e) {
                throw new MailboxException("connecting to mail store failed", e);
            }
        }

        return store;
    }

    /**
     * Wrapper around javax.mail.Session.getInstance()
     */
    public Session getSessionInstance(Properties props, Authenticator authenticator) {
        return Session.getInstance(props, null);
    }

    /**
     * Get the inbox directory.
     */
    public Folder getInbox() throws MailboxException {
        if (inbox == null || !inbox.isOpen()) {
            try {
                // Get the INBOX folder.
                inbox = getStore().getFolder("INBOX");

                // We need to open it READ_WRITE,
                // because we want to move/delete messages we already handled.
                inbox.open(Folder.READ_WRITE);
            } catch (FolderNotFoundException fnfe) {
                throw new MailboxException("could not find INBOX folder", fnfe);
            } catch (MessagingException e) {
                throw new MailboxException("could not open INBOX folder", e);
            }
        }

        return inbox;
    }

    /**
     * Get the default directory.
     */
    public Folder getDefaultFolder() throws MailboxException {
        if (defaultFolder == null || !defaultFolder.isOpen()) {
            try {
                // Get the defaultFolder.
                defaultFolder = getStore().getDefaultFolder();

                // We need to open it READ_WRITE (There is no write only),
                // because we want to move/delete messages we already handled.
                defaultFolder.open(Folder.READ_WRITE);
            } catch (FolderNotFoundException fnfe) {
                throw new MailboxException("could not find ROOT folder", fnfe);
            } catch (MessagingException e) {
                throw new MailboxException("could not open ROOT folder", e);
            }
        }

        return defaultFolder;
    }

    /**
     * Get all messages in the INBOX folder.
     */
    public Message[] getMessages() throws MailboxException {
        try {
            return getInbox().getMessages();
        } catch (MessagingException e) {
            throw  new MailboxException("could not fetch messages from inbox", e);
        }
    }

    public int getCount() throws MailboxException {
        try {
            return getInbox().getMessageCount();
        } catch (MessagingException e) {
            throw  new MailboxException("failed to count messages in inbox", e);
        }
    }

    /**
     * Close the mailbox.
     */
    public void close() throws MailboxException {
        if (inbox != null && inbox.isOpen()) {
            try {
                inbox.close(true);
            } catch (MessagingException e) {
                throw new MailboxException("failed to close INBOX folder", e);
            }
        }

        if (defaultFolder != null && defaultFolder.isOpen()) {
            try {
                defaultFolder.close(true);
            } catch (MessagingException e) {
                throw new MailboxException("failed to close ROOT folder", e);
            }
        }

        if (store != null) {
            try {
                store.close();
            } catch (MessagingException e) {
                throw new MailboxException("failed to close mail store", e);
            }
        }
    }

    /**
     * Mark message as processed.
     */
    public void flagAsProcessed(Message message) throws MailboxException {
        getFlagStrategy().flagAsProcessed(message);
    }

    /**
     * Mark message as invalid.
     */
    public void flagAsInvalid(Message message) throws MailboxException {
        getFlagStrategy().flagAsInvalid(message);
    }
}
