package ut.de.dm.mail2blog;

import de.dm.mail2blog.GlobalState;
import de.dm.mail2blog.MailConfiguration;
import de.dm.mail2blog.MailConfigurationManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class GlobalStateTest
{
    GlobalState globalState;
    MailConfigurationManager configurationManager;
    MailConfiguration mailConfiguration;

    @Before
    public void setUp() {
        globalState = new GlobalState();
        configurationManager = mock(MailConfigurationManager.class);

        mailConfiguration = MailConfiguration.builder()
            .username("alice")
            .emailaddress("alice@example.org")
            .build();

        when(configurationManager.loadConfig()).thenReturn(mailConfiguration);

        globalState.setMailConfigurationManager(configurationManager);
    }

    @Test
    public void test() throws Exception {
        // Make sure config is fetched from config manager on the first try.
        MailConfiguration mailConfigurationFirstTime = globalState.getMailConfigurationWrapper().getMailConfiguration();
        assertEquals("Failed to get config", mailConfiguration, mailConfigurationFirstTime);
        verify(configurationManager, times(1)).loadConfig();

        // Make sure config is fetched from memory on the second try.
        MailConfiguration mailConfigurationSecondTime = globalState.getMailConfigurationWrapper().getMailConfiguration();
        assertEquals("Failed to get config", mailConfiguration, mailConfigurationSecondTime);
        verify(configurationManager, times(1)).loadConfig();

        // Set config to null and check config is fetched from config manager again.
        globalState.setMailConfigurationWrapper(null);
        MailConfiguration mailConfigurationThirdTime = globalState.getMailConfigurationWrapper().getMailConfiguration();
        assertEquals("Failed to get config", mailConfiguration, mailConfigurationThirdTime);
        verify(configurationManager, times(2)).loadConfig();
    }
}
