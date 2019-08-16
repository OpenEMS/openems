package io.openems.common.utils;

import io.openems.common.exceptions.OpenemsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    public static StringBuilder checkAndGetFileContent(String path) throws OpenemsException {
        if (path == null || path.isEmpty()) {
            log.info("No path given - so no content can be loaded!");
            return null;
        }

        // read file
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            log.warn("Unable to read file [" + path + "]: " + e.getMessage());
            throw new OpenemsException(e);
        }
        return sb;
    }
}
