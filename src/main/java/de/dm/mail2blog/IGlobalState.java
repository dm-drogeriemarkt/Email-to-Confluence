package de.dm.mail2blog;

public interface IGlobalState {
    MailConfigurationWrapper getMailConfigurationWrapper();
    void setMailConfigurationWrapper(MailConfigurationWrapper mailConfigurationWrapper);
}
