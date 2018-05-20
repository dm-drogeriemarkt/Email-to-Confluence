package de.dm.mail2blog;

import com.atlassian.confluence.spaces.Space;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpaceInfo {
    private Space space;
    private String contentType; // ContentTypes
}
