package ut.de.dm.mail2blog;

import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.user.Group;
import com.atlassian.user.GroupManager;
import com.atlassian.user.search.page.Pager;
import de.dm.mail2blog.GlobalState;
import de.dm.mail2blog.MailConfiguration;
import de.dm.mail2blog.MailConfigurationWrapper;
import de.dm.mail2blog.actions.CheckboxTracker;
import de.dm.mail2blog.actions.ConfigurationAction;
import de.dm.mail2blog.actions.ConfigurationActionState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class ConfigurationActionTest
{
    private static final String SPACE1_KEY = "de";
    private static final String SPACE1_NAME = "Germany";

    private static final String SPACE2_KEY = "fr";
    private static final String SPACE2_NAME = "France";

    private static final String GROUP1 = "Engineers";
    private static final String GROUP2 = "Cooks";

    GroupManager groupManager = mock(GroupManager.class);
    SpaceManager spaceManager = mock(SpaceManager.class);

    ConfigurationAction configurationAction;
    MailConfiguration mailConfiguration;
    CheckboxTracker checkboxTracker;

    private void setUpConfigurationAction() throws Exception {
        configurationAction = new ConfigurationAction();
        configurationAction.setGroupManager(groupManager);
        configurationAction.setSpaceManager(spaceManager);
        configurationAction.setCheckboxTracker(checkboxTracker);

        mailConfiguration = MailConfiguration.builder().build();
        MailConfigurationWrapper wrapper = new MailConfigurationWrapper(mailConfiguration);

        GlobalState globalState = mock(GlobalState.class);
        when(globalState.getMailConfigurationWrapper()).thenReturn(wrapper);

        ConfigurationActionState actionState = new ConfigurationActionState();
        actionState.setGlobalState(globalState);

        configurationAction.setConfigurationActionState(actionState);
    }

    private void setUpSpaceManager() throws Exception {
        Space spaceA = mock(Space.class);
        when(spaceA.getKey()).thenReturn(SPACE1_KEY);
        when(spaceA.getName()).thenReturn(SPACE1_NAME);
        when(spaceManager.getSpace(SPACE1_KEY)).thenReturn(spaceA);

        Space spaceB = mock(Space.class);
        when(spaceB.getKey()).thenReturn(SPACE2_KEY);
        when(spaceB.getName()).thenReturn(SPACE2_NAME);
        when(spaceManager.getSpace(SPACE2_KEY)).thenReturn(spaceB);

        ArrayList<Space> spaces = new ArrayList<Space>();
        spaces.add(spaceA);
        spaces.add(spaceB);

        when(spaceManager.getAllSpaces()).thenReturn(spaces);
    }

    private void setUpGroupManager() throws Exception {
        Group groupA = mock(Group.class);
        when(groupA.getName()).thenReturn(GROUP1);
        when(groupManager.getGroup(GROUP1)).thenReturn(groupA);

        Group groupB = mock(Group.class);
        when(groupB.getName()).thenReturn(GROUP2);
        when(groupManager.getGroup(GROUP2)).thenReturn(groupB);

        final ArrayList<Group> groups = new ArrayList<Group>();
        groups.add(groupA);
        groups.add(groupB);

        Pager<Group> pager = mock(Pager.class);
        when(pager.isEmpty()).thenReturn(groups.isEmpty());
        when(pager.iterator()).thenReturn(groups.iterator());

        when(groupManager.getGroups()).thenReturn(pager);
    }

    private void setUpCheckboxTracker() {
        checkboxTracker = new CheckboxTracker();
    }

    @Before
    public void setUp() throws Exception {
        setUpCheckboxTracker();
        setUpSpaceManager();
        setUpGroupManager();
        setUpConfigurationAction();
    }

    /**
     * Check that getGroups works.
     */
    @Test
    public void testGetGroups() throws Exception
    {
        assertEquals(
            "Failed to get groups",
            Arrays.asList(new String[] {GROUP1, GROUP2}),
            configurationAction.getGroups()
        );
    }

    /**
     * Check that getSpaces works.
     */
    @Test
    public void testGetSpaces() throws Exception
    {
        Hashtable<String, String> hashtable_spaces = new Hashtable<String, String>();
        hashtable_spaces.put(SPACE1_KEY, SPACE1_NAME);
        hashtable_spaces.put(SPACE2_KEY, SPACE2_NAME);

        assertEquals(
            "Failed to get spaces",
            hashtable_spaces,
            configurationAction.getSpaces()
        );
    }

    /**
     * Check that getProtocols works.
     */
    @Test
    public void testGetProtocols() throws Exception
    {
        ConfigurationAction action = new ConfigurationAction();
        Map<String, String> map = action.getProtocols();
        assertEquals(map.get("imap"), "IMAP");
        assertEquals(map.get("pop3"), "POP3");
    }

    @Test
    public void testPreferred()
    {
        configurationAction.setPreferred("text");
        assertEquals("text", configurationAction.getPreferred());
        assertArrayEquals(
            new String[]{"text/plain", "text/html"},
            configurationAction.getMailConfiguration().getPreferredContentTypes()
        );

        configurationAction.setPreferred("html");
        assertEquals("html", configurationAction.getPreferred());
        assertArrayEquals(
            new String[]{"text/html", "text/plain"},
            configurationAction.getMailConfiguration().getPreferredContentTypes()
        );
    }

    /**
     * Test the validation process with true and false values.
     */
    @Test
    public void testValidate() throws Exception {
        assertValidate("mailConfiguration.server", "localhost", true);
        assertValidate("mailConfiguration.server", "", false);
        assertValidate("mailConfiguration.server", "123~123.de", false);
        assertValidate("mailConfiguration.username", "johndoe", true);
        assertValidate("mailConfiguration.username", "", false);
        assertValidate("mailConfiguration.emailaddress", "alice@example.org", true);
        assertValidate("mailConfiguration.emailaddress", "", false);
        assertValidate("mailConfiguration.protocol", "imap", true);
        assertValidate("mailConfiguration.protocol", "pop3", true);
        assertValidate("mailConfiguration.protocol", "imaps", false);
        assertValidate("mailConfiguration.protocol", "http", false);
        assertValidate("mailConfiguration.protocol", "", false);
        assertValidate("mailConfiguration.port", 143, true);
        assertValidate("mailConfiguration.port", 70000, false);
        assertValidate("mailConfiguration.port", -1, false);
        assertValidate("mailConfiguration.defaultSpace", SPACE1_KEY, true);
        assertValidate("mailConfiguration.defaultSpace", SPACE2_KEY, true);
        assertValidate("mailConfiguration.defaultSpace", "bogus", false);
        assertValidate("mailConfiguration.defaultSpace", "", false);
        assertValidate("mailConfiguration.maxAllowedAttachmentSize", 123, true);
        assertValidate("mailConfiguration.maxAllowedAttachmentSize", -1, false);
        assertValidate("mailConfiguration.maxAllowedAttachmentSize", 3000, false);
        assertValidate("mailConfiguration.maxAllowedNumberOfAttachments", -1, true);
        assertValidate("mailConfiguration.maxAllowedNumberOfAttachments", 0, true);
        assertValidate("mailConfiguration.maxAllowedNumberOfAttachments", 10, true);
        assertValidate("mailConfiguration.maxAllowedNumberOfAttachments", -2, false);
        assertValidate("mailConfiguration.allowedFileTypes", "jpg image/jpeg\npng image/png", true);
        assertValidate("mailConfiguration.allowedFileTypes", "jpg image/jpeg\npng image", false);
        assertValidate("mailConfiguration.securityGroup", "", true);
        assertValidate("mailConfiguration.securityGroup", GROUP1, true);
        assertValidate("mailConfiguration.securityGroup", GROUP2, true);
        assertValidate("mailConfiguration.securityGroup", "bogus", false);
    }


    /**
     * Set a value on a html field. Call the validation routine. Check the result.
     */
    private void assertValidate(String field, Object value, boolean expectedResult) throws Exception {
        // Set the value.
        setValue(field, value);

        // Reset errors and validate.
        configurationAction.setFieldErrors(new HashMap<String, String>());
        configurationAction.validate();

        // Look for an error message for the field.
        boolean validationResult = true;
        for (Object key : configurationAction.getFieldErrors().keySet()) {
            if (field.equals(key)) {
                validationResult = false;
            }
        }

        if (expectedResult) {
            assertTrue(
                "Got an unexpected error msg on field '" + field + "' for value '" + value + "'.",
                validationResult
            );
        } else {
            assertFalse(
                "Expected an error msg on field '" + field + "' for value '" + value + "'.",
                validationResult
            );
        }
    }

    /**
     * Call a setter by the html field name.
     * Mimics the way confluence will call setters after form submission.
     *
     * @param field The html name of the field
     * @param value The value
     */
    private void setValue(String field, Object value) throws Exception {

        // Split field by dot.
        String[] parts = field.split("\\.");

        // Call getters till the last element in field is reached.
        // firstObject.secondObject.field -> getFirstObject().getSecondObject().
        Object object = configurationAction;
        for (int i = 0; i < (parts.length -1); i++) {
            String property = parts[i];

            // Get descriptors of all properties.
            BeanInfo info = Introspector.getBeanInfo(object.getClass());
            PropertyDescriptor[] descriptors = info.getPropertyDescriptors();

            // Find the property with the right name.
            boolean found_property = false;
            for (PropertyDescriptor descriptor : descriptors) {
                if(property.equals(descriptor.getName())) {
                    Method getter = descriptor.getReadMethod();
                    if (getter != null) {
                        object = getter.invoke(object);
                        found_property = true;
                        break;
                    } else {
                        fail("No getter found for property '" + property + "' in field '" + field + "'.");
                    }
                }
            }

            assertTrue("Failed to find property '" + property + "' in field '" + field + "'.", found_property);
        }

        // The last element in field is the setter.
        String property = parts[parts.length - 1];

        // Get descriptors of all properties.
        BeanInfo info = Introspector.getBeanInfo(object.getClass());
        PropertyDescriptor[] descriptors = info.getPropertyDescriptors();

        // Find the property with the right name
        // and call the setter with value
        boolean found_property = false;
        for (PropertyDescriptor descriptor : descriptors) {
            if(property.equals(descriptor.getName())) {
                Method setter = descriptor.getWriteMethod();
                if (setter != null) {
                    descriptor.getWriteMethod().invoke(object, value);
                    found_property = true;
                    break;
                } else {
                    fail("No setter found for property '" + property + "' in field '" + field + "'.");
                }
            }
        }
        assertTrue("Failed to find property '" + property + "' in field '" + field + "'.", found_property);
    }
}
