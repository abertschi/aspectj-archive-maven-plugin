package ch.abertschi.aspectj;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * 
 * Util to resolve artifact from Maven.
 * 
 */
public class AetherUtils
{
	private RepositorySystem repoSystem;

	private RepositorySystemSession repoSession;

	private List<RemoteRepository> remoteRepos;

	public AetherUtils(RepositorySystem repoSystem, RepositorySystemSession repoSession,
			List<RemoteRepository> remoteRepos)
	{

		this.repoSystem = repoSystem;
		this.repoSession = repoSession;
		this.remoteRepos = remoteRepos;

	}

	public List<ArtifactResult> resolveWithTrancivity(String gav)
	{
		Artifact artifact;
		try
		{
			artifact = new DefaultArtifact(gav);
		} catch (IllegalArgumentException e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}

		ArtifactRequest request = new ArtifactRequest();
		request.setArtifact(artifact);
		request.setRepositories(remoteRepos);

		DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
		DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter);

		List<ArtifactResult> artifactResults = new ArrayList<ArtifactResult>();
		try
		{
			artifactResults = repoSystem.resolveDependencies(repoSession, dependencyRequest).getArtifactResults();

		} catch (DependencyResolutionException e1)
		{
			throw new RuntimeException(e1);
		}

		return artifactResults;
	}

	public static List<JavaArchive> transform(List<ArtifactResult> in)
	{
		List<JavaArchive> out = new ArrayList<JavaArchive>();
		for (ArtifactResult artifactResult : in)
		{
			String artifactName = String.format("%s.%s", 
					artifactResult.getArtifact().getArtifactId(), 
					artifactResult.getArtifact().getExtension());

			JavaArchive javaArchive = ShrinkWrap.create(ZipImporter.class, artifactName)
					.importFrom(artifactResult.getArtifact().getFile())
					.as(JavaArchive.class);
			
			out.add(javaArchive);

		}
		return out;
	}
}
