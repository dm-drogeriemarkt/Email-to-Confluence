package de.dm.mail2blog;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

/**
 * Autowired object that stores the mail configuration in use and that can load/store the configuration
 * in the plugin settings.
 */
@Slf4j
public class MailConfigurationManager implements IMailConfigurationManager {

    public static final String PLUGIN_KEY = "de.dm.mail2blog";

    // Autowired objects.
    @Setter PluginSettingsFactory pluginSettingsFactory;

    public MailConfiguration loadConfig()
    {
        MailConfiguration mailConfiguration = null;

        try {
            PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
            ObjectMapper objectMapper = new ObjectMapper();

            Object object = pluginSettings.get(PLUGIN_KEY);
            if (object instanceof Map) {
                Map<String, Object> map = (Map) object;
                mailConfiguration = (MailConfiguration) objectMapper.convertValue(map, MailConfiguration.class);
            } else if (object != null) {
                log.error("Mail2Blog: Failed to load config. Invalid type returned.");
            }
        } catch (Exception e) {
            mailConfiguration = null;
            log.error("Mail2Blog: Failed to load config.", e);
        }

        if (mailConfiguration == null) {
            mailConfiguration = MailConfiguration.builder().build();
        }

        return mailConfiguration;
    }

    /**
     * Save the configuration by saving the mailConfiguration object as map in plugin settings.
     */
    public void saveConfig(@NonNull MailConfiguration mailConfiguration)
    throws MailConfigurationManagerException
    {
        try {
            PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
            ObjectMapper objectMapper = new ObjectMapper();

            Map<String, Object> map = objectMapper.convertValue(mailConfiguration, Map.class);
            pluginSettings.put(PLUGIN_KEY, map);
        } catch (Exception e) {
            throw new MailConfigurationManagerException("Failed to save configuration", e);
        }

        // Read back the configuration from storage and make sure that it equals the given config.
        if (!mailConfiguration.equals(loadConfig())) {
            throw new MailConfigurationManagerException("Failed to save configuration.");
        }

    }
}
