package de.dm.mail2blog;

import com.atlassian.confluence.spaces.Space;
import javax.mail.Message;
import java.util.*;

import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.spring.container.ContainerManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SpaceFactory {

    /**
     * Try to find suitable spaces given the config of this plugin and an E-Mail message.
     *
     * @param mailConfigurationWrapper The configuration to use to determine the strategies for space key extraction
     * @param message The email to find spaces for.
     * @return A list of suitable spaces. Ordered by relevance.
     */
    public static List<Space> getSpace(MailConfigurationWrapper mailConfigurationWrapper, Message message) {
        ArrayList<ISpaceKeyExtractor> extractors = new ArrayList<ISpaceKeyExtractor>();
        ArrayList<String> space_keys = new ArrayList<String>();
        ArrayList<Space> spaces = new ArrayList<Space>();

        // Get space manger.
        SpaceManager spaceManager = (SpaceManager) ContainerManager.getComponent("spaceManager");

        // Add space extractors.
        if (mailConfigurationWrapper.getMailConfiguration().getSpaceKeyInAddress()) {
            extractors.add(new VERPSpaceKeyExtractor());
        }
        if (mailConfigurationWrapper.getMailConfiguration().getSpaceKeyInSubject()) {
            extractors.add(new SubjectSpaceKeyExtractor());
        }
        if (mailConfigurationWrapper.getMailConfiguration().getDefaultSpace().length() != 0) {
            extractors.add(new DefaultSpaceKeyExtractor());
        }

        // Get space keys.
        for (ISpaceKeyExtractor extractor : extractors) {
            space_keys.addAll(extractor.getSpaceKeys(mailConfigurationWrapper, message));
        }

        // Check if a space can be found for given space key.
        for (String space_key : space_keys) {
            Space space = spaceManager.getSpace(space_key);
            if (space == null) {
                log.debug("Mail2Blog: Unknown space key: " + space_key);
            } else {
                spaces.add(space);
            }
        }

        return spaces;
    }

}
