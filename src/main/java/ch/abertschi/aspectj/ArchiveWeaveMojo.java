package ch.abertschi.aspectj;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.aspectj.Module;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;

/**
 * Maven Old Java Object that is capable of compile-time-weaving (CTW) complete EARs or WARs
 * using {@code aspecj-maven-plugin} by {@code org.codehaus.mojo} under the hooks.
 *
 * @author Andrin Bertsch
 * @since 2015-05
 */
@Mojo(name = "archive-weave", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE)
public class ArchiveWeaveMojo extends AbstractMojo implements AjConfigurable {

	@Component
	private MavenProject mavenProject;

	@Component
	private MavenSession mavenSession;

	@Component
	private BuildPluginManager pluginManager;

	//-------------------------------------------------------------------------------------||
	// mojo configuration -----------------------------------------------------------------||
	//-------------------------------------------------------------------------------------||

	@Parameter(required = true, readonly = true, defaultValue = "${basedir}")
	protected File basedir;

	@Parameter(readonly = true, defaultValue = "${project.build.directory}")
	private File outputDirectory;

	///**
	//* Maven settings.xml required for dependency resolution.
	//*/
	@Parameter(required = true, defaultValue = "${env.M2_HOME}/conf/settings.xml")
	protected String mavenConf;

	/**
	 * Archive to import and enrich for full deployment. (relative path from current dir).
	 */
	@Parameter(required = true)
	protected String archiveImport;

	/**
	 * Path to the full deployment that will be created.
	 */
	@Parameter(defaultValue = "${project.build.directory}/${project.artifact.artifactId}-${project.artifact.version}.${project.artifact.type}")
	private String archiveExport;

	//-------------------------------------------------------------------------------------||
	// aspectj configurations -------------------------------------------------------------||
	//-------------------------------------------------------------------------------------||

	@Parameter(defaultValue = "1.7")
	protected String ajMojoVersion;

	/**
	 * List of of modules to weave (into target directory). Corresponds to <code>ajc
	 * -inpath</code> option (or <code>-injars</code> for pre-1.2 (which is not supported)).
	 */
	@Parameter
	protected Module[] weaveDependencies;

	/**
	 * Weave binary aspects from the jars.
	 * The aspects should have been output by the same version of the compiler.
	 * The modules must also be dependencies of the project.
	 * Corresponds to <code>ajc -aspectpath</code> option
	 */
	@Parameter(required = true)
	protected Module[] aspectLibraries;

	/**
	 * Specify compiler compliance setting.
	 * Defaults to 1.4, with permitted values ("1.3", "1.4", "1.5", "1.6" and "1.7", "1.8").
	 *
	 * @see org.codehaus.mojo.aspectj.AjcHelper#ACCEPTED_COMPLIANCE_LEVEL_VALUES
	 */
	@Parameter(defaultValue = "1.7")
	protected String complianceLevel = "1.7";

	/**
	 * Forces re-compilation, regardless of whether the compiler arguments or the sources have changed.
	 */
	@Parameter()
	protected boolean forceAjcCompile = true;

	/**
	 * Set default level for messages about potential programming mistakes in crosscutting code. {level} may be ignore,
	 * warning, or error. This overrides entries in org/aspectj/weaver/XlintDefault.properties from aspectjtools.jar.
	 */
	@Parameter()
	protected String Xlint = "ignore";

	/**
	 * Emit messages about accessed/processed compilation units
	 */
	@Parameter
	protected boolean verbose = false;

	/**
	 * Emit messages about weaving
	 */
	@Parameter
	protected boolean showWeaveInfo = false;

	//-------------------------------------------------------------------------------------||
	// execution --------------------------------------------------------------------------||
	//-------------------------------------------------------------------------------------||

	public void execute() throws MojoExecutionException {
		getLog().info("Started goal archive-weave ...");

		File importArchiveFile = Utils.getFile(basedir, archiveImport);
		String archiveName = Utils.getFilename(archiveImport);
        ArchiveType ext = ArchiveType.getExtensionFromName(archiveName);

		failIfNotSupportedExtension(ext);

		LibraryContainer<? extends Archive<?>> weaveArchive;
		getLog().info("Importing base artifact [" + archiveName + "] from [" + importArchiveFile.getAbsolutePath() + "]");

		if (ext == ArchiveType.EAR) {
			weaveArchive = importArchive(EnterpriseArchive.class, importArchiveFile, archiveName);
		} else {
			weaveArchive = importArchive(WebArchive.class, importArchiveFile, archiveName);
		}

		getLog().info("Resolving transient dependencies of aspectLibraries");
		//List<JavaArchive> libs = resolveDependencies(aspectLibraries);
		//weaveArchive.addAsLibraries(libs);

		getLog().info("Compile time weaving jars in artifact");
		List<JavaArchive> recompiledJars = recompileJars();

		Archive<? extends Archive<?>> oldArchive = weaveArchive.addAsLibraries(new ArrayList<Archive<?>>());
		Archive<? extends Archive<?>> newArchive = replaceJars(oldArchive, recompiledJars);

		Utils.mkdirIfNotExists(mavenProject.getBuild().getDirectory());
		//newArchive.addManifest();
		newArchive.as(ZipExporter.class).exportTo(new File(archiveExport), true);

		getLog().info("Weaved archive created [" + archiveExport + "]");
	}

	//-------------------------------------------------------------------------------------||
	// archive operation section ----------------------------------------------------------||
	//-------------------------------------------------------------------------------------||

	protected List<JavaArchive> resolveDependencies(Module[] dependencies) {
		PomEquippedResolveStage resolver = Maven.configureResolver()
						.workOffline()
						.fromFile(mavenConf)
						.loadPomFromFile("pom.xml")
						.importCompileAndRuntimeDependencies();

		List<JavaArchive> jars = new ArrayList<JavaArchive>();
		for (Module module: dependencies) {
			// todo: is check necessary to determine same artifact from multiple libs? yes!
			final String identifier = module.getGroupId() + ":" + module.getArtifactId();
			jars.addAll(Arrays.asList(resolver.resolve(identifier).withTransitivity().as(JavaArchive.class)));
		}
		return jars;
	}

	protected <T extends Archive<T>> T importArchive(Class<T> type, File location, String name) {
		return ShrinkWrap
				.create(ZipImporter.class, name)
				.importFrom(location)
				.as(type);
	}

	private List<JavaArchive> importDependencies(Module[] dependencies) {
		List<JavaArchive> jars = new ArrayList<JavaArchive>();
		try {
			/*
			 * To simplify dependency management, we import
			 * all dependencies of the test maven module (compile + test),
			 * That makes dependency management much simpler.
			 */
			jars = Arrays.asList(Maven
					.configureResolver().fromFile(mavenConf)
					.offline()
					.loadPomFromFile("pom.xml")
					.importDependencies(ScopeType.COMPILE) // TODO: test only might be ok as well
					.resolve()
					.withTransitivity()
					.as(JavaArchive.class));
		} catch (IllegalArgumentException e) {
			/*
			 * No dependencies were set for resolution.
			 * Thus, no test jars will be added to archive.
			 */
			getLog().info("No test dependencies were set for resolution");
		}
		return jars;
	}

	private List<JavaArchive> recompileJars() {
		final String ajCompileDir = mavenProject.getBuild().getDirectory() + "/ajc";
		Utils.mkdirIfNotExists(ajCompileDir);

		AjCompiler compiler = new AjCompiler(this);
		compiler.setOutputDirectory(ajCompileDir);

		List<JavaArchive> recompiledJars = new ArrayList<JavaArchive>();
		for (Module toWeave : weaveDependencies) {
			getLog().info("Recompiling library [" + toWeave.getGroupId() + ":" + toWeave.getArtifactId() );

			recompiledJars.add(compiler.recompile(toWeave, aspectLibraries));
		}
		return recompiledJars;
	}

	private <T extends Archive<T>> Archive<T> replaceJars(Archive<T> archive, List<JavaArchive> newJars) {
		Archive<T> newArchive = archive.shallowCopy();
		Map<ArchivePath, Node> archiveContent = archive.getContent();

		for (Entry<ArchivePath, Node> entry : archiveContent.entrySet()) {
			String artifactName = Utils.getFilename(entry.getKey().get());
			if (ArchiveType.JAR == ArchiveType.getExtensionFromName(artifactName)) {
				getLog().debug("Adding jar [" + artifactName + "]");

				for (JavaArchive recompiled : newJars) {
					if (artifactName.equals(recompiled.getName())) {
						getLog().info("Replacing jar [" + entry.getKey().get() + "] with recompiled jar ");

						newArchive.delete(entry.getKey());
						String withoutFilename = Utils.getPathWithoutFilename(entry.getKey().get());
						newArchive.add(recompiled, withoutFilename, ZipExporter.class);
					}
				}
			}
		}
		return newArchive;
	}

	//-------------------------------------------------------------------------------------||
	// helper section ---------------------------------------------------------------------||
	//-------------------------------------------------------------------------------------||

    protected void failIfNotSupportedExtension(ArchiveType ext) throws MojoExecutionException {
        if (ext == ArchiveType.UNKNOWN) {
            throw new MojoExecutionException("Not recognised file extension in " + archiveImport);
        }
    }

	//-------------------------------------------------------------------------------------||
	// AjConfigurable ---------------------------------------------------------------------||
	//-------------------------------------------------------------------------------------||

	@Override
	public String getAjMojoVersion() {
		return ajMojoVersion;
	}

	@Override
	public boolean isShowWeaveInfo() {
		return showWeaveInfo;
	}

	@Override
	public Module[] getWeaveDependencies() {
		return weaveDependencies;
	}

	@Override
	public Module[] getAspectLibraries() {
		return aspectLibraries;
	}

	@Override
	public String getComplianceLevel() {
		return complianceLevel;
	}

	@Override
	public boolean isForceAjcCompile() {
		return forceAjcCompile;
	}

	@Override
	public String getXlint() {
		return Xlint;
	}

	@Override
	public boolean isVerbose() {
		return verbose;
	}

	@Override
	public MavenProject getMavenProject() {
		return mavenProject;
	}

	@Override
	public MavenSession getMavenSession() {
		return mavenSession;
	}

	@Override
	public BuildPluginManager getPluginManager() {
		return pluginManager;
	}
}
