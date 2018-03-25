package de.dm.mail2blog;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.spring.container.ContainerManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Autowired object that stores the mail configuration in use and that can load/store the configuration
 * in the plugin settings.
 */
@Slf4j
@Component
@ExportAsService
public class MailConfigurationManager {
    public static final String PLUGIN_KEY = "de.dm.mail2blog";

    public MailConfiguration loadConfig()
    {
        MailConfiguration mailConfiguration = null;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ConfluenceBandanaContext ctx = newGlobalConfluenceBandaContext();
            Object object = getBandanaManager().getValue(ctx, PLUGIN_KEY);
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
            ConfluenceBandanaContext ctx = newGlobalConfluenceBandaContext();
            ObjectMapper objectMapper = new ObjectMapper();

            Map<String, Object> map = objectMapper.convertValue(mailConfiguration, Map.class);
            getBandanaManager().setValue(ctx, PLUGIN_KEY, map);
        } catch (Exception e) {
            throw new MailConfigurationManagerException("Failed to save configuration", e);
        }

        // Read back the configuration from storage and make sure that it equals the given config.
        if (!mailConfiguration.equals(loadConfig())) {
            throw new MailConfigurationManagerException("Failed to save configuration.");
        }

    }

    // PluginSettingsManager seems to be broken in newer confluence versions.
    // This is why bandana manager is used directly.
    // Couldn't get Autowiring to work for BandanaManager.
    public BandanaManager getBandanaManager() {
        return (BandanaManager)ContainerManager.getComponent("bandanaManager");
    }

    public ConfluenceBandanaContext newGlobalConfluenceBandaContext() {
        return new ConfluenceBandanaContext((Space) null);
    }
}
