package ut.de.dm.mail2blog;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext;
import de.dm.mail2blog.MailConfiguration;
import de.dm.mail2blog.MailConfigurationManager;
import de.dm.mail2blog.SpaceRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MailConfigurationManagerTest
{
    private @Mock ConfluenceBandanaContext ctx;
    private @Mock BandanaManager bandanaManager;

    private MailConfigurationManager mailConfigurationManager;

    @Before
    public void setUp() throws Exception {
        mailConfigurationManager = spy(new MailConfigurationManager());
        doReturn(bandanaManager).when(mailConfigurationManager).getBandanaManager();
        doReturn(ctx).when(mailConfigurationManager).newGlobalConfluenceBandaContext();
    }

    @Test
    public void testSaveConfig() throws Exception {
        MailConfiguration configuration = MailConfiguration.builder()
            .username("alice")
            .emailaddress("alice@example.org")
            .spaceRules(new SpaceRule[]{SpaceRule.builder().field("from").operator("is").value("alice@example.org").action("copy").space("test").build()})
            .build();

        // Mock loadConfig(), because saveConfig() will verify the result by calling loadConfig().
        doReturn(configuration).when(mailConfigurationManager).loadConfig();

        mailConfigurationManager.saveConfig(configuration);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(bandanaManager).setValue(eq(ctx), eq(MailConfigurationManager.PLUGIN_KEY), captor.capture());

        assertEquals("alice", captor.getValue().get("username"));
        assertEquals("alice@example.org", captor.getValue().get("emailaddress"));

        ArrayList<Map<String, Object>> spaceRules = (ArrayList<Map<String, Object>>) captor.getValue().get("spaceRules");
        assertEquals(1, spaceRules.size());
        assertEquals("from", spaceRules.get(0).get("field"));
        assertEquals("is", spaceRules.get(0).get("operator"));
        assertEquals("alice@example.org", spaceRules.get(0).get("value"));
        assertEquals("copy", spaceRules.get(0).get("action"));
        assertEquals("test", spaceRules.get(0).get("space"));
    }

   @Test
   public void testLoadConfig() throws Exception {
       Map<String, Object> map = new HashMap<String, Object>();
       map.put("username", "bob");
       map.put("password", "alice123");

       map.put("spaceRules", new ArrayList<Map<String, Object>>(){
           {
               add(new HashMap<String, Object>() {
                   {
                       put("field", "from");
                       put("operator", "is");
                       put("value", "alice@example.org");
                       put("action", "copy");
                       put("space", "test");
                   }
               });
           }
       });

       when(bandanaManager.getValue(ctx, MailConfigurationManager.PLUGIN_KEY)).thenReturn(map);

       MailConfiguration configuration = mailConfigurationManager.loadConfig();

       assertEquals("bob", configuration.getUsername());
       assertEquals("alice123", configuration.getPassword());

       SpaceRule[] spaceRules = configuration.getSpaceRules();
       assertEquals(1, spaceRules.length);
       assertEquals("from", spaceRules[0].getField());
       assertEquals("is", spaceRules[0].getOperator());
       assertEquals("alice@example.org", spaceRules[0].getValue());
       assertEquals("copy", spaceRules[0].getAction());
       assertEquals("test", spaceRules[0].getSpace());
   }
}
