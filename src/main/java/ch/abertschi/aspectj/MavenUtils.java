package ch.abertschi.aspectj;

import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

public class MavenUtils
{
	private MavenUtils()
	{
		throw new UnsupportedOperationException("Dont instanciate me!");
	}

	/**
	 * Searches the given maven project for an artifact matching the groupId, artifactId and type.
	 * Useful to get version of an artifact.
	 * 
	 * @param mavenProject
	 * @param groupId
	 * @param artifactId
	 * @param type
	 * @return
	 */
	public static Artifact resolveArtifact(MavenProject mavenProject, String groupId, String artifactId, String type)
	{
		Set<Artifact> allArtifacts = mavenProject.getArtifacts();
		for (Artifact art : allArtifacts)
		{
			if (art.getGroupId().equals(groupId) && art.getArtifactId().equals(artifactId)
					&& StringUtils.defaultString(null).equals(StringUtils.defaultString(art.getClassifier()))
					&& StringUtils.defaultString(type, "jar").equals(StringUtils.defaultString(art.getType())))
			{
				return art;
			}
		}
		return null;
	}
}
