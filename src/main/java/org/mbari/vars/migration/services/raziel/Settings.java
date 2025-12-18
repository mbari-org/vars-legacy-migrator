/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.services.raziel;

import org.mbari.vars.migration.util.crypto.AES;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Settings {

    private static Path settingsDirectory;

    private static final System.Logger log = System.getLogger(Settings.class.getName());

    public static AES getAes() {
        return new AES("brian@mbari.org 1993-08-21");
    }

    /**
     * The settingsDirectory is scratch space for VARS
     *
     * @return The path to the settings directory. null is returned if the
     *  directory doesn't exist (or can't be created) or is not writable.
     */
    public static Path getSettingsDirectory() {
        if (settingsDirectory == null) {
            String home = System.getProperty("user.home");
            Path path = Paths.get(home, ".vars");
            settingsDirectory = createDirectory(path);
            if (settingsDirectory == null) {
                log.log(System.Logger.Level.WARNING, "Failed to create settings directory at " + path);
            }
        }
        return settingsDirectory;
    }

    public static Path createDirectory(Path path) {
        Path createdPath = path;
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
                if (!Files.isWritable(path)) {
                    createdPath = null;
                }
            }
            catch (IOException e) {
                log.log(System.Logger.Level.WARNING, () -> "Unable to create a directory at " + path + ".", e);
                createdPath = null;
            }
        }
        return createdPath;
    }
}
