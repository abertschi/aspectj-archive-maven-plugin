package ch.abertschi.aspectj;

/**
 * @author Andrin Bertsch
 * @since 2015-05
 */
public enum ArchiveType
{
    EAR,
    WAR,
    JAR,
    UNKNOWN;

    /**
     * Get Instance by file name.
     *
     * @param name a file having a file extension.
     * @return file extension or IllegalArgumentException if not supported argument.
     */
    public static ArchiveType getExtensionFromFilename(String name)
    {
        ArchiveType ext = UNKNOWN;
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
        return ext;
    }

    public static ArchiveType getExtensionFromString(String name)
    {
        ArchiveType ext = UNKNOWN;
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
        
        return ext;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
