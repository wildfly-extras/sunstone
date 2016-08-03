package org.wildfly.extras.sunstone.api.impl;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FilesUtils {
    private static final Logger LOGGER = SunstoneCoreLogger.DEFAULT;

    private FilesUtils() {} // avoid instantiation

    /**
     * Open file on a system path. If not found, then search on the classpath.
     *
     * @param filePath system path or classpath
     * @return InputStream of opened file or <code>null</code> if the file is not found.
     * @throws IOException if opening file on system path fails
     */
    public static InputStream openFile(String filePath) throws IOException {
        if (filePath == null) {
            return null;
        }
        final Path path = Paths.get(filePath);
        if (Files.isRegularFile(path)) {
            return Files.newInputStream(path);
        } else {
            LOGGER.trace("Path {} doesn't denote a regular file, looking for a classpath resource");
            InputStream is = FilesUtils.class.getResourceAsStream(filePath);
            if (is == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
            }
            if (is != null) {
                LOGGER.debug("Path {} doesn't denote a regular file, but a classpath resource was found");
            }
            return is;
        }
    }
}
