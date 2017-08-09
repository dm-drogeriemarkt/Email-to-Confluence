package de.dm.mail2blog;

import lombok.NonNull;
import lombok.extern.log4j.Log4j;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores the allowed mime types and file extensions and supports serializing/deserializing to tab seperated CSV
 * to store the values in BANDANA and to display them in the admin UI.
 */
@Log4j
public class FileTypeBucket {

    // Map that maps mimeType to file extensions.
    private HashMap<String, Set<String>> storage;

    // Regex to extract the file extension and mime type from a CSV row.
    private static final Pattern PATTERN = Pattern.compile(
        "^\\s*" +
        "([a-zA-Z0-9]+)" + // Extension
        "\\s+" +
        "([a-zA-Z0-9]+/[a-zA-Z0-9.+\\-]+)" + // Mime Type
        "\\s*$"
    );

    /**
     * Private constructor to create the bucket from hashMap.
     * Use @see FileTypeBucket.fromString() to create a FileTypeBucket object.
     */
    private FileTypeBucket(@NonNull HashMap<String, Set<String>> storage) {
        this.storage = storage;
    }

    /**
     * Parse csv string and create bucket from it.
     *
     * @param data
     *  Space separated CSV with the extension as the first column and the mimeType as the second.
     *
     * @throws FileTypeBucketException
     *  On Syntax error in CSV
     */
    public static FileTypeBucket fromString(@NonNull String data)
    throws FileTypeBucketException
    {
        HashMap<String, Set<String>> storage = new HashMap<String, Set<String>>();
        Scanner scanner = new Scanner(data);

        int line_nr = 1;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Matcher match = PATTERN.matcher(line);
            if (match.find()) {
                String extension = match.group(1).trim().toLowerCase();
                String mimeType = match.group(2).trim().toLowerCase();

                Set<String> extensions = storage.getOrDefault(mimeType, new HashSet<String>());
                extensions.add(extension);
                storage.put(mimeType, extensions);
            } else {
                throw new FileTypeBucketException("Syntax error in line " + line_nr + " near \"" + line + "\"");
            }

            line_nr++;
        }

        return new FileTypeBucket(storage);
    }

    /**
     * Serialize bucket to CSV.
     *
     * @return
     *  Space separated CSV with the extension as the first column and the mimeType as second.
     */
    public String toString() {
        String result = "";

        for (Entry<String, Set<String>> entry : storage.entrySet()) {
            String mimeType = entry.getKey();
            for (String extension : entry.getValue()) {
                result += extension + " " + mimeType + "\n";
            }
        }

        return result;
    }

    /**
     * Removes potentially dangerous characters from  given file name
     * and check that the file extension matches the given mimeType.
     * If not it appends a matching extension.
     *
     * @warning If the mimeType is unknown no extension will be allowed
     * use FileTypeBucket.checkMimeType() to verify that the extension is allowed.
     *
     * @param filename The file name to sanitize
     * @param mimeType The mime type of the file
     * @return sanitized file name
     */
    public String saneFilename(@NonNull String filename, @NonNull String mimeType) {
        // Replace exotic characters with _.
        filename = filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");

        // Determine if we need to append an extension,
        // or if the filename already contains a valid one.
        Set<String> validExtensions = storage.get(mimeType);
        if (validExtensions != null && !validExtensions.isEmpty()) {
            boolean appendExtension = false;
            int index = filename.lastIndexOf('.');
            if (index > 0 && index < (filename.length() - 1)) {
                String extension = filename.substring(index + 1).toLowerCase();
                if (!validExtensions.contains(extension)) {
                    appendExtension = true;
                }
            } else {
                appendExtension = true;
            }

            if (appendExtension) {
                filename += "." + validExtensions.iterator().next();
            }
        }

        return filename;
    }

    /**
     * Check if mimeType is in the list of known mime types.
     *
     * @param mimeType The mime type to check.
     */
    public boolean checkMimeType(String mimeType) {
        return storage.containsKey(mimeType);
    }
}
