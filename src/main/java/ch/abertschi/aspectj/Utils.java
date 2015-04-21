package ch.abertschi.aspectj;
import java.io.File;

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
		if (! archiveFile.exists()) {
			throw new RuntimeException("Specified deployable doesnt exist: " + base.getAbsolutePath() + " name: " + suffix);
		}
		return archiveFile;
	}
}
