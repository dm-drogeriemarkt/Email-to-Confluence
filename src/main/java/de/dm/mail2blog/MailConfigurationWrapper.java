package de.dm.mail2blog;

import lombok.Setter;

/**
 * Wrapper around a mail configuration bean, that provides additional methods and
 * properties created at runtime.
 */
public class MailConfigurationWrapper {

    /**
     * The actual mail configuration being wrapped.
     */
    @Setter MailConfiguration mailConfiguration;

    /**
     * A file type bucket created from mailConfiguration.allowedFileTypes.
     */
    FileTypeBucket fileTypeBucket;

    /**
     * The allowedFileTypes used to generate the fileTypeBucket.
     */
    String allowedFileTypes = "";

    public MailConfigurationWrapper(MailConfiguration mailConfiguration) {
        this.mailConfiguration = mailConfiguration;
    }

    /**
     * Clone the underlying mailConfiguration bean and create a new Wrapper with it.
     *
     * @return Wrapper with cloned mailConfiguration.
     */
    public MailConfigurationWrapper duplicate() {
        return new MailConfigurationWrapper(mailConfiguration.toBuilder().build());
    }

    public FileTypeBucket getFileTypeBucket()
    throws FileTypeBucketException
    {
        if (
            fileTypeBucket == null
            || !mailConfiguration.getAllowedFileTypes().equals(allowedFileTypes)
        ) {
            allowedFileTypes = mailConfiguration.getAllowedFileTypes();
            fileTypeBucket = FileTypeBucket.fromString(allowedFileTypes);
        }

        return fileTypeBucket;
    }

    public void setFileTypeBucket(FileTypeBucket fileTypeBucket) {
        this.fileTypeBucket = fileTypeBucket;
        mailConfiguration.setAllowedFileTypes(fileTypeBucket.toString());
    }

    public MailConfiguration getMailConfiguration() {
        UpgradeMailConfiguration.upgradeMailConfiguration(mailConfiguration);
        return mailConfiguration;
    }
}