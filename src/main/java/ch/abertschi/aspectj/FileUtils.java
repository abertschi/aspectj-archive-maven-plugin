package ch.abertschi.aspectj;

import java.io.File;

/**
 * File utils
 * 
 * @author Andrin Bertsch
 * @since 2015-05
 * 
 */
public class FileUtils
{

    private FileUtils()
    {
    }

    public static void makeDirsIfNotExist(String directory)
    {
        File buildDir = new File(directory);
        if (!buildDir.exists())
        {
            buildDir.mkdirs();
        }
    }

    public static File getFileOrFail(File base, String suffix)
    {
        File archiveFile = new File(base, suffix);
        return getFileOrFail(archiveFile);
    }
    
    public static File getFileOrFail(File archiveFile)
    {
        if (!archiveFile.exists())
        {
            throw new RuntimeException("File does not exist: " + archiveFile.getAbsolutePath());
        }
        return archiveFile;
    }

    public static String getPathWithoutFilename(String path)
    {
        int index = path.lastIndexOf("/");
        if (index == 0)
        {
            return "/";
        }
        else
        {
            return path.substring(0, index);
        }
    }

    public static String getFilename(String path)
    {
        String result;
        String[] split = path.split("/");
        if (split.length > 1)
        {
            result = split[split.length - 1];
        }
        else
        {
            result = split[0];
        }
        return result;
    }
    
    public static String getNameWithoutExtension(String name)
    {
        String result;
        String[] split = name.split("\\.");
        if (split.length > 1)
        {
            result = split[split.length - 1];
            result = name.substring(name.length() - result.length());
        }
        else
        {
            result = split[0];
        }
        return result;
    }
}
