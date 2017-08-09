package ut.de.dm.mail2blog;

import com.atlassian.spring.container.ContainerManager;
import de.dm.mail2blog.FileTypeBucket;
import de.dm.mail2blog.FileTypeBucketException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ContainerManager.class})
public class FileTypeBucketTest
{

    /**
     * Check that testing for mime type works.
     */
    @Test
    public void testCheckMimeType() throws Exception
    {
        FileTypeBucket fileTypeBucket = FileTypeBucket.fromString("jpg  image/jpeg\ngif image/gif");

        assertTrue("Jpeg should be allowed", fileTypeBucket.checkMimeType("image/jpeg"));
        assertFalse("application/octet-stream shouldn't be allowed", fileTypeBucket.checkMimeType("application/octet-stream"));
    }

    /**
     * Check that file names are properly generated.
     */
    @Test
    public void testFileNameGeneration() throws Exception
    {
        FileTypeBucket fileTypeBucket = FileTypeBucket.fromString("jpg  image/jpeg\ngif image/gif");

        assertEquals("dm-logo.jpg", fileTypeBucket.saneFilename("dm-logo.jpg", "image/jpeg"));
        assertEquals("dm-logo.exe.jpg", fileTypeBucket.saneFilename("dm-logo.exe", "image/jpeg"));
        assertEquals("dm_-___logo.gif", fileTypeBucket.saneFilename("dm -()?logo", "image/gif"));
        assertEquals("html", fileTypeBucket.saneFilename("html", "text/html"));
    }

    /**
     * Check that an exception is thrown when an string with invalid syntax is given.
     */
    @Test
    public void testInvalidSyntax() throws Exception
    {
        boolean exception = false;

        try {
            FileTypeBucket.fromString("png  image/png\njpg image");
        } catch (FileTypeBucketException e) {
            exception = true;
        }

        assertTrue("Exception should have been thrown on invalid syntax.", exception);
    }

    /**
     * Test that serializing the bucket to string works as expected.
     */
    @Test
    public void testToString() throws Exception
    {
        FileTypeBucket source = FileTypeBucket.fromString("png  image/png\njpg image/jpeg");
        FileTypeBucket copy = FileTypeBucket.fromString(source.toString());

        assertTrue("Jpeg should be allowed", copy.checkMimeType("image/jpeg"));
        assertTrue("Png should be allowed", copy.checkMimeType("image/png"));
        assertFalse("Gif shouldn't be allowed", copy.checkMimeType("image/gif"));
    }
}

