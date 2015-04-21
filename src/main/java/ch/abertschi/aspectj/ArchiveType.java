package ch.abertschi.aspectj;

/**
 * @author Andrin Bertschi
 */
public enum ArchiveType {
    EAR,
    WAR,
    JAR,
    UNKNOWN;

    public static ArchiveType getExtensionFromName(String name) {
        ArchiveType ext = ArchiveType.UNKNOWN;
        if (name.toLowerCase().endsWith(".ear")) {
            ext = ArchiveType.EAR;
        } else if (name.toLowerCase().endsWith(".war")) {
            ext = ArchiveType.WAR;
        } else if (name.toLowerCase().endsWith(".jar")) {
            ext = ArchiveType.JAR;
        }
        return ext;
    }
}
