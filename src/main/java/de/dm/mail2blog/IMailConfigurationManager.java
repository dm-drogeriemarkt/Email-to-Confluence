package de.dm.mail2blog;

public interface IMailConfigurationManager {
    public MailConfiguration loadConfig();
    public void saveConfig(MailConfiguration mailConfiguration) throws MailConfigurationManagerException;
}
