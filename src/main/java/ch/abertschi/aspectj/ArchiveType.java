package ch.abertschi.aspectj;

import com.sun.javaws.exceptions.InvalidArgumentException;

/**
 * @author Andrin Bertsch
 * @since 2015-05
 */
public enum ArchiveType
{
    EAR,
    WAR,
    JAR;


    /**
     * Get Instance by file name.
     *
     * @param name a file having a file extension.
     * @return file extension or IllegalArgumentException if not supported argument.
     */
    public static ArchiveType getExtensionFromFilename(String name)
    {
        ArchiveType ext;
        if (name.toLowerCase().endsWith(".ear"))
        {
            ext = ArchiveType.EAR;
        }
        else if (name.toLowerCase().endsWith(".war"))
        {
            ext = ArchiveType.WAR;
        }
        else if (name.toLowerCase().endsWith(".jar"))
        {
            ext = ArchiveType.JAR;
        }
        else
        {
            throw new IllegalArgumentException(String.format("File with unsupported file extension %s. Valid are EAR, WAR and JAR.", name));
        }
        return ext;
    }

    public static ArchiveType getExtensionFromString(String name)
    {
        ArchiveType ext;
        if (name.toLowerCase().equals("ear"))
        {
            ext = ArchiveType.EAR;
        }
        else if (name.toLowerCase().equals("war"))
        {
            ext = ArchiveType.WAR;
        }
        else if (name.toLowerCase().equals("jar"))
        {
            ext = ArchiveType.JAR;
        }
        else
        {
            throw new IllegalArgumentException(String.format("Unsupported file extension %s. Valid are EAR, WAR and JAR.", name));
        }

        return ext;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
