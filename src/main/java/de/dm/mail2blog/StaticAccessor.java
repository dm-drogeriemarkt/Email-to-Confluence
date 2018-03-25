package de.dm.mail2blog;

import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.user.GroupManager;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// Some component imports don't work in jobs or in tests. But can be accessed through static properties.
// See: https://developer.atlassian.com/server/framework/atlassian-sdk/access-components-statically/
@Component
public class StaticAccessor {
    private static @Getter @Setter TransactionTemplate transactionTemplate;
    private static @Getter @Setter SpaceManager spaceManager;
    private static @Getter @Setter AttachmentManager attachmentManager;
    private static @Getter @Setter PageManager pageManager;
    private static @Getter @Setter GroupManager groupManager;
    private static @Getter @Setter SettingsManager settingsManager;
    private static @Getter @Setter UserAccessor userAccessor;
    private static @Getter @Setter MailConfigurationManager mailConfigurationManager;
    private static @Getter @Setter Mail2BlogJob mail2BlogJob;
    private static @Getter @Setter GlobalState globalState;

    @Autowired
    public StaticAccessor(
        @ComponentImport TransactionTemplate transactionTemplate,
        @ComponentImport SpaceManager spaceManager,
        @ComponentImport AttachmentManager attachmentManager,
        @ComponentImport PageManager pageManager,
        @ComponentImport GroupManager groupManager,
        @ComponentImport SettingsManager settingsManager,
        @ComponentImport UserAccessor userAccessor,
        MailConfigurationManager mailConfigurationManager,
        Mail2BlogJob mail2BlogJob,
        GlobalState globalState
    ) {
        setTransactionTemplate(transactionTemplate);
        setSpaceManager(spaceManager);
        setAttachmentManager(attachmentManager);
        setPageManager(pageManager);
        setGroupManager(groupManager);
        setSettingsManager(settingsManager);
        setUserAccessor(userAccessor);
        setMailConfigurationManager(mailConfigurationManager);
        setMail2BlogJob(mail2BlogJob);
        setGlobalState(globalState);
    }
}
