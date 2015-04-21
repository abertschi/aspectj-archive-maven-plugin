package ch.abertschi.aspectj;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;

// put dependency resolution into this class
// general maven resolution things

public class ArchiveOperator<T extends Archive<T>> {
	
	public static class Builder {
		
		private String settingsXml;
		
		private String pomXml;
		
		public Builder(String settingsXml, String pomXml) {
			this.settingsXml = settingsXml;
			this.pomXml = pomXml;
		}
		
		public ArchiveOperator<EnterpriseArchive> forEar() {
			ArchiveOperator<EnterpriseArchive> op =  
					new ArchiveOperator<EnterpriseArchive>(EnterpriseArchive.class);
			
			op.settingsXml = settingsXml;
			op.pomXml = pomXml;
			op.init();
			
			return op;
		}
	}
	
	private Class<T> type;
	
	private String settingsXml;
	
	private String pomXml;

	private PomEquippedResolveStage resolver;
	
	private ArchiveOperator(Class<T> type) {
		this.type = type;
	}
	
	private void init() {
		resolver = Maven
	        .configureResolver().fromFile(settingsXml)
	        .offline()
	        .loadPomFromFile("pom.xml");
	}
	
	public T importFrom(File from, String name) {
		return ShrinkWrap
				.create(ZipImporter.class, name)
				.importFrom(from)
				.as(type);
	}
	
	public T replaceJarsWith(T archive, List<JavaArchive> jars) {
		T newArchive = (T) archive.shallowCopy();
        Map<ArchivePath, Node> archiveContent = archive.getContent();
        
        for (Entry<ArchivePath, Node> entry : archiveContent.entrySet()) {
        	String artifactName = extractFilename(entry.getKey().get());
        	if (artifactName.endsWith(".jar")) {
        		// getLog().debug("Adding jar [" + artifactName + "]");
        		
        		for(JavaArchive replaceJar: jars) {
        			if (artifactName.equals(replaceJar.getName())) {
        				// getLog().info("Replacing jar [" + entry.getKey().get() + "] with recompiled jar in archive ");
        				
        				newArchive.delete(entry.getKey());
        				String withoutFilename = pathWithoutFilename(entry);
        				newArchive.add(replaceJar, withoutFilename, ZipExporter.class);
        			}
        		}
			}
		}
		return newArchive;
	}
	
	public PomEquippedResolveStage getResolver() {
		return this.resolver;
	}
	
	private String pathWithoutFilename(Entry<ArchivePath, Node> entry) {
		final String entryPath = entry.getKey().get();
		int index = entryPath.lastIndexOf("/");
		if (index == 0){
			return "/";
		}
		else {
			return entryPath.substring(0, index);
		}
	}
	
	private String extractFilename(String path) {
		String result;
		String [] split = path.split("/");
		if (split.length > 1) {
			result = split[split.length - 1];
		}
		else {
			result = split[0];
		}
		return result;
	}
}
