package ut.de.dm.mail2blog;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import de.dm.mail2blog.MailConfiguration;
import de.dm.mail2blog.MailConfigurationManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
public class MailConfigurationManagerTest
{
    private PluginSettingsFactory pluginSettingsFactory;
    private PluginSettings globalSettings;
    private MailConfigurationManager mailConfigurationManager;

    @Before
    public void setUp() throws Exception {
        pluginSettingsFactory = mock(PluginSettingsFactory.class);
        globalSettings = mock(PluginSettings.class);
        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(globalSettings);

        mailConfigurationManager = spy(new MailConfigurationManager());
        mailConfigurationManager.setPluginSettingsFactory(pluginSettingsFactory);
    }

    @Test
    public void testSaveConfig() throws Exception {
        MailConfiguration configuration = MailConfiguration.builder()
            .username("alice")
            .emailaddress("alice@example.org")
            .build();

        // Mock loadConfig(), because saveConfig() will verify the result by calling loadConfig().
        doReturn(configuration).when(mailConfigurationManager).loadConfig();

        mailConfigurationManager.saveConfig(configuration);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(globalSettings).put(eq(MailConfigurationManager.PLUGIN_KEY), captor.capture());

        assertTrue("Username not saved", captor.getValue().containsKey("username"));
        assertEquals("alice", captor.getValue().get("username"));
        assertTrue("Email not saved", captor.getValue().containsKey("emailaddress"));
        assertEquals("alice@example.org", captor.getValue().get("emailaddress"));
    }

    @Test
    public void testLoadConfig() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("username", "bob");
        map.put("password", "alice123");

        when(globalSettings.get(MailConfigurationManager.PLUGIN_KEY)).thenReturn(map);

        MailConfiguration configuration = mailConfigurationManager.loadConfig();

        assertEquals("bob", configuration.getUsername());
        assertEquals("alice123", configuration.getPassword());
    }
}
