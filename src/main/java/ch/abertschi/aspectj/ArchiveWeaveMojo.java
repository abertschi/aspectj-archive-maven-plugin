package ch.abertschi.aspectj;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
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

/**
 * Maven Old Java Object that is capable of compile-time-weaving (CTW) complete EARs or WARs
 * using {@code aspecj-maven-plugin} by {@code org.codehaus.mojo} under the hooks.
 *
 * @author Andrin Bertsch
 * @since 2015-05
 */

@Mojo(name = "archive-weave", requiresDependencyResolution = 
	  ResolutionScope.TEST, defaultPhase = LifecyclePhase.PACKAGE)
public class ArchiveWeaveMojo extends AbstractMojo implements AjConfigurable
{

    @Component
    private MavenProject mavenProject;

    @Component
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Component
    private ArtifactFactory artifactFactory;
    
    @Component
    private RepositorySystem repoSystem;

	@Parameter(defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repoSession;

	@Parameter(defaultValue = "${project.remoteProjectRepositories}")
    private List<RemoteRepository> remoteRepos;

    //-------------------------------------------------------------------------------------||
    // mojo configuration -----------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    @Parameter(required = true, readonly = true, defaultValue = "${basedir}")
    protected File basedir;

    @Parameter(readonly = true, defaultValue = "${project.build.directory}")
    private File outputDirectory;

    /**
     * Maven settings.xml required for dependency resolution.
     */
    @Parameter(required = true, defaultValue = "${env.M2_HOME}/conf/settings.xml")
    protected String mavenConf;

    /**
     * Archive to import and enrich for full deployment. (relative path from current dir).
     */
    @Parameter(required = true)
    protected String archiveImport;

    /**
     * Path to the full deployment that will be created.
     *
     * default value:
     * "./target/${project.artifact.artifactId}-${project.artifact.version}.${extension of file in archiveImport}"
     */
    @Parameter()
    private String archiveExport = null;

    //-------------------------------------------------------------------------------------||
    // aspectj configurations -------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    @Parameter(defaultValue = "1.7")
    protected String ajMojoVersion;

    /**
     * Aj config: <br />
     * List of of modules to weave (into target directory). Corresponds to <code>ajc
     * -inpath</code> option (or <code>-injars</code> for pre-1.2 (which is not supported)).
     */
    @Parameter
    protected Module[] weaveDependencies;

    /**
     * Aj config: <br />
     * Weave binary aspects from the jars.
     * The aspects should have been output by the same version of the compiler.
     * The modules must also be dependencies of the project.
     * Corresponds to <code>ajc -aspectpath</code> option
     */
    @Parameter(required = true)
    protected Module[] aspectLibraries;

    /**
     * Aj config: <br />
     * Specify compiler compliance setting.
     * Defaults to 1.4, with permitted values ("1.3", "1.4", "1.5", "1.6" and "1.7", "1.8").
     *
     * @see org.codehaus.mojo.aspectj.AjcHelper#ACCEPTED_COMPLIANCE_LEVEL_VALUES
     */
    @Parameter(defaultValue = "1.7")
    protected String complianceLevel = "1.7";

    /**
     * Aj config: <br />
     * Forces re-compilation, regardless of whether the compiler arguments or the sources have changed.
     */
    @Parameter()
    protected boolean forceAjcCompile = true;

    /**
     * Aj config: <br />
     * Set default level for messages about potential programming mistakes in crosscutting code. {level} may be ignore,
     * warning, or error. This overrides entries in org/aspectj/weaver/XlintDefault.properties from aspectjtools.jar.
     */
    @Parameter()
    protected String Xlint = "ignore";

    /**
     * Aj config: <br />
     * Emit messages about accessed/processed compilation units
     */
    @Parameter
    protected boolean verbose = false;

    /**
     * Aj config: <br />
     * Emit messages about weaving
     */
    @Parameter
    protected boolean showWeaveInfo = false;

    //-------------------------------------------------------------------------------------||
    // execution --------------------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    public void execute() throws MojoExecutionException
    {
        getLog().info("Executing mojo aspectj-archive-maven-plugin with goal archive-weave ...");

        File importArchiveFile = Utils.getFileOrFail(basedir, archiveImport);
        String archiveName = Utils.getFilename(archiveImport);

        ArchiveType ext = ArchiveType.getExtensionFromFilename(archiveName);
        getLog().info("Archive type [" + ext + "] recognised");

        if (archiveExport == null || archiveExport.isEmpty())
        {
            getLog().info("No archiveExport file specified. Using defaults ...");
            archiveExport = String.format("%s-%s.%s", mavenProject.getArtifactId(), mavenProject.getVersion(), ext.toString() );
        }

        LibraryContainer<? extends Archive<?>> weaveArchive;
        getLog().info("Importing base artifact [" + archiveName + "] from [" + importArchiveFile.getAbsolutePath() + "] ...");

        if (ext == ArchiveType.EAR)
        {
            weaveArchive = importArchive(EnterpriseArchive.class, importArchiveFile, archiveName);
        }
        else if (ext == ArchiveType.WAR)
        {
            weaveArchive = importArchive(WebArchive.class, importArchiveFile, archiveName);
        }
        else
        {
            throw new IllegalArgumentException("Other file extensions than EAR or WAR currently not supported");
        }

        getLog().info("Resolving transient dependencies of aspectLibraries ...");
        List<JavaArchive> libs = resolveDependencies(aspectLibraries);
        weaveArchive.addAsLibraries(libs);

        getLog().info("Compile-time-weaving weaveDependencies ...");
        List<JavaArchive> recompiledJars = recompileJars();

        Archive<? extends Archive<?>> oldArchive = weaveArchive.addAsLibraries(new ArrayList<Archive<?>>());
        Archive<? extends Archive<?>> newArchive = replaceJars(oldArchive, recompiledJars);

        Utils.makeDirsIfNotExist(mavenProject.getBuild().getDirectory());
        
        File exportArchiveFile = new File(basedir, archiveExport);
        newArchive.as(ZipExporter.class).exportTo(exportArchiveFile, true);

        getLog().info("Compile-time-weaved artifact created [" + archiveExport + "].");
    }

    //-------------------------------------------------------------------------------------||
    // archive operation section ----------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    protected List<JavaArchive> resolveDependencies(Module[] dependencies)
    {
    	AetherUtils resolver = new AetherUtils(repoSystem, repoSession, remoteRepos);
    			
        List<JavaArchive> jars = new ArrayList<JavaArchive>();
        for (Module module : dependencies)
        {
        	Artifact resolveArtifact = MavenUtils.resolveArtifact(mavenProject,
        			module.getGroupId(),
        			module.getArtifactId(), null);
        	
        	if (resolveArtifact == null)
        	{
        		throw new RuntimeException(String.format("Given dependency %s:%s could not be "
        				+ "found in maven project",  module.getGroupId(), module.getArtifactId()));
        	}
        			
            final String gav = String.format("%s:%s:%s",
            		module.getGroupId(), 
            		module.getArtifactId(),
            		resolveArtifact.getVersion());
            
            List<ArtifactResult> artifacts = resolver.resolveWithTrancivity(gav);
            List<JavaArchive> jarsForModule = AetherUtils.transform(artifacts);
            
            for (JavaArchive j : jarsForModule)
            {
                getLog().info("Added required dependency [" + j + "] to archive.");
            }
            
            // todo: exclude artifacts already present
            jars.addAll(jarsForModule);
        }
        return jars;
    }

    protected <T extends Archive<T>> T importArchive(Class<T> type, File location, String name)
    {
        return ShrinkWrap
                .create(ZipImporter.class, name)
                .importFrom(location)
                .as(type);
    }

    private List<JavaArchive> recompileJars()
    {
        final String ajCompileDir = mavenProject.getBuild().getDirectory() + "/ajc";
        Utils.makeDirsIfNotExist(ajCompileDir);

        AjCompiler compiler = new AjCompiler(this);
        compiler.setOutputDirectory(ajCompileDir);

        List<JavaArchive> recompiledJars = new ArrayList<JavaArchive>();
        for (Module toWeave : weaveDependencies)
        {
            getLog().info("Recompiling library [" + toWeave.getGroupId() + ":" + toWeave.getArtifactId());
            recompiledJars.add(compiler.recompile(toWeave, aspectLibraries));
        }
        return recompiledJars;
    }

    private <T extends Archive<T>> Archive<T> replaceJars(Archive<T> archive, List<JavaArchive> newJars)
    {
        Archive<T> newArchive = archive.shallowCopy();
        Map<ArchivePath, Node> archiveContent = archive.getContent();

        for (Entry<ArchivePath, Node> entry : archiveContent.entrySet())
        {
            String artifactName = Utils.getFilename(entry.getKey().get());
            if (ArchiveType.JAR == ArchiveType.getExtensionFromFilename(artifactName))
            {
                getLog().debug("Adding jar [" + artifactName + "]");

                for (JavaArchive recompiled : newJars)
                {
                    if (artifactName.equals(recompiled.getName()))
                    {
                        getLog().info("Replacing jar [" + entry.getKey().get() + "] with compile-time-weaved equivalent.");

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

    //-------------------------------------------------------------------------------------||
    // AjConfigurable ---------------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||
    
    @Override
    public String getAjMojoVersion()
    {
        return ajMojoVersion;
    }

    @Override
    public boolean isShowWeaveInfo()
    {
        return showWeaveInfo;
    }

    @Override
    public Module[] getWeaveDependencies()
    {
        return weaveDependencies;
    }

    @Override
    public Module[] getAspectLibraries()
    {
        return aspectLibraries;
    }

    @Override
    public String getComplianceLevel()
    {
        return complianceLevel;
    }

    @Override
    public boolean isForceAjcCompile()
    {
        return forceAjcCompile;
    }

    @Override
    public String getXlint()
    {
        return Xlint;
    }

    public boolean isVerbose()
    {
        return verbose;
    }

    @Override
    public MavenProject getMavenProject()
    {
        return mavenProject;
    }

    @Override
    public MavenSession getMavenSession()
    {
        return mavenSession;
    }

    @Override
    public BuildPluginManager getPluginManager()
    {
        return pluginManager;
    }
}
