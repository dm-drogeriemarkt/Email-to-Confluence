package de.dm.mail2blog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class GlobalStateTest
{
    GlobalState globalState;
    @Mock MailConfigurationManager configurationManager;
    @Mock MailConfiguration mailConfiguration;

    @Before
    public void setUp() {
        initMocks(this);

        globalState = new GlobalState();
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
