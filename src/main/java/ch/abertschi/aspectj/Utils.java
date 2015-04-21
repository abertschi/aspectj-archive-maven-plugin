package ch.abertschi.aspectj;

import java.io.File;

/**
 * @author Andrin Bertsch
 * @since 2015-05
 */
public class Utils {

    private Utils() {
    }

    public static void mkdirIfNotExists(String directory) {
        File buildDir = new File(directory);
        if (!buildDir.exists()) {
            buildDir.mkdir();
        }
    }

    public static File getFile(File base, String suffix) {
        File archiveFile = new File(base, suffix);
        if (!archiveFile.exists()) {
            throw new RuntimeException("Specified deployable doesnt exist: " + base.getAbsolutePath() + " name: " + suffix);
        }
        return archiveFile;
    }

    public static String getPathWithoutFilename(String path) {
        int index = path.lastIndexOf("/");
        if (index == 0) {
            return "/";
        } else {
            return path.substring(0, index);
        }
    }

    public static String getFilename(String path) {
        String result;
        String[] split = path.split("/");
        if (split.length > 1) {
            result = split[split.length - 1];
        } else {
            result = split[0];
        }
        return result;
    }
}
