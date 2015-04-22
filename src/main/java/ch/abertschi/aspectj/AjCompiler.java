package ch.abertschi.aspectj;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;


/**
 * A wrapper around the {@code org.codehaus.mojo:aspectj-maven-plugin}.
 * Is capable of compile-time-weaving (CTW) maven modules with given aspect libraries.
 *
 * @author Andrin Bertschi
 * @since 2015-04
 */
public class AjCompiler {

    private AjConfigurable ajConfig;
    private String savedOutputDir;

    private String buildBaseDir;
    private String savedSourceDir;
    private String savedTestOutputDir;

    public AjCompiler(AjConfigurable ajConfig) {
        this.ajConfig = ajConfig;
        this.buildBaseDir = ajConfig.getMavenProject().getBuild().getDirectory() + "/ajc";
        Utils.mkdirIfNotExists(buildBaseDir);
    }

    public JavaArchive recompile(Module source, Module[] aspectLibraries) throws RuntimeException {
        backupMavenProject();

        final Artifact resolvedArtifact = resolveArtifact(source);
        final String artifactId = source.getArtifactId();
        final String version = source.getVersion() != null ? source.getVersion() : resolvedArtifact.getVersion();
        final String type = source.getType() != null ? source.getType() : resolvedArtifact.getType();

        final String finalName = createArtifactName(artifactId, version, type);
        final String outputDirectory = buildBaseDir + "/" + artifactId;
        final String compiledClassesDir = outputDirectory + "/classes";

        Utils.mkdirIfNotExists(compiledClassesDir);
        changeMavenProject(compiledClassesDir);
        List<Element> aspectModules = generateAspectXmlModules(aspectLibraries);

        try {
            executeMojo(
                    plugin(
                            groupId("org.codehaus.mojo"),
                            artifactId("aspectj-maven-plugin"),
                            version(ajConfig.getAjMojoVersion())
                    ),
                    goal("compile"),
                    configuration(
                            element("aspectLibraries", aspectModules.toArray(new Element[0])),
                            element("weaveDependencies", createModule(source, "weaveDependency")),
                            element("forceAjcCompile", String.valueOf(ajConfig.isForceAjcCompile())),
                            element("Xlint", ajConfig.getXlint()),
                            element("verbose", String.valueOf(ajConfig.isVerbose())),
                            element("showWeaveInfo", String.valueOf(ajConfig.isShowWeaveInfo()))
                    ),
                    executionEnvironment(
                            ajConfig.getMavenProject(),
                            ajConfig.getMavenSession(),
                            ajConfig.getPluginManager()
                    )
            );

        } catch (MojoExecutionException e) {
            throw new RuntimeException(e);
        }

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, finalName)
                .as(ExplodedImporter.class).importDirectory(new File(compiledClassesDir))
                .as(JavaArchive.class);

        jar.as(ZipExporter.class).exportTo(new File(outputDirectory + "/" + jar.getName()), true);

        restoreMavenProject();

        return jar;
    }

    //-------------------------------------------------------------------------------------||
    // maven project ----------------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    private void backupMavenProject() {
        final Build build = ajConfig.getMavenProject().getBuild();
        savedSourceDir = build.getSourceDirectory();
        savedTestOutputDir = build.getTestOutputDirectory();
        savedOutputDir = build.getOutputDirectory();
    }

    /*
     * Force aspectJ compiler to recompile only weaving dependencies and not
     * source/ test-source of project.
     */
    private void changeMavenProject(String compiledClassesDir) {
        final Build build = ajConfig.getMavenProject().getBuild();
        build.setSourceDirectory("$dummy");
        build.setTestOutputDirectory("$dummy");
        build.setOutputDirectory(compiledClassesDir);
    }

    private void restoreMavenProject() {
        final Build build = ajConfig.getMavenProject().getBuild();
        build.setSourceDirectory(savedSourceDir);
        build.setTestOutputDirectory(savedTestOutputDir);
        build.setOutputDirectory(savedOutputDir);
    }

    public String getOutputDirectory() {
        return buildBaseDir;
    }

    public void setOutputDirectory(String buildDir) {
        buildBaseDir = buildDir;
    }

    //-------------------------------------------------------------------------------------||
    // private section --------------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    private String createArtifactName(String artifactId, String version, String type) {
        return artifactId + "-" + version + "." + type;
    }

    private List<Element> generateAspectXmlModules(Module[] aspectLibraries) {
        List<Element> aspectModules = new ArrayList<Element>();
        for (Module element : aspectLibraries) {
            aspectModules.add(createModule(element, "aspectLibrary"));
        }
        return aspectModules;
    }

    private Element createModule(Module module, String name) {
        return MojoExecutor.element(name,
                MojoExecutor.element("groupId", module.getGroupId()),
                MojoExecutor.element("artifactId", module.getArtifactId()),
                MojoExecutor.element("classifier", module.getClassifier()),
                MojoExecutor.element("type", module.getType()));
    }


    private Artifact resolveArtifact(Module module) {
        Set<Artifact> allArtifacts = ajConfig.getMavenProject().getArtifacts();
        for (Artifact art : allArtifacts) {
            if (art.getGroupId().equals(module.getGroupId())
                    && art.getArtifactId().equals(module.getArtifactId())
                    && StringUtils.defaultString(module.getClassifier()).equals(StringUtils.defaultString(art.getClassifier()))
                    && StringUtils.defaultString(module.getType(), "jar").equals(StringUtils.defaultString(art.getType()))) {
                return art;
            }
        }
        return null;
    }
}
