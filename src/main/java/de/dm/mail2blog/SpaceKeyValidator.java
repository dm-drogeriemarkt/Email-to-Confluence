package de.dm.mail2blog;

import com.atlassian.confluence.spaces.SpaceManager;
import de.dm.mail2blog.base.ISpaceKeyValidator;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.stereotype.Component;

@Component
public class SpaceKeyValidator implements ISpaceKeyValidator {
    private SpaceManager spaceManager;

    @Autowired
    public SpaceKeyValidator(
        @ComponentImport @NonNull SpaceManager spaceManager
    ) {
        this.spaceManager = spaceManager;
    }

    @Override
    public boolean spaceExists(String s) {
        return (spaceManager.getSpace(s) != null);
    }
}
