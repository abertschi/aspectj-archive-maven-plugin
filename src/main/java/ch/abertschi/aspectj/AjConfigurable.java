package ch.abertschi.aspectj;


import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.aspectj.Module;

/**
 * Supported flags for the AspectJ Mojo.
 *
 * @author Andrin Bertsch
 * @since 2015-05
 */
public interface AjConfigurable {

    String getAjMojoVersion();

    boolean isShowWeaveInfo();

    Module[] getWeaveDependencies();

    Module[] getAspectLibraries();

    String getComplianceLevel();

    boolean isForceAjcCompile();

    String getXlint();

    boolean isVerbose();

    MavenProject getMavenProject();

    MavenSession getMavenSession();

    BuildPluginManager getPluginManager();
}
