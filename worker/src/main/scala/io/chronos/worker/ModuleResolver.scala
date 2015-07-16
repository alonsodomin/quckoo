package io.chronos.worker

import io.chronos.id.ModuleId
import org.eclipse.aether.artifact.{Artifact, DefaultArtifact}
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository.Builder
import org.eclipse.aether.resolution.{ArtifactRequest, ArtifactResult, DependencyRequest}
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.filter.DependencyFilterUtils
import org.eclipse.aether.{RepositorySystem, RepositorySystemSession}

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 16/07/15.
 */
object ModuleResolver {
  val Central = new Builder("central", "default", "http://central.maven.org/maven2/").build()
}

class ModuleResolver(val system: RepositorySystem, val session: RepositorySystemSession) {
  import ModuleResolver._

  def resolve(moduleId: ModuleId): Artifact = {
    val unresolvedArtifact = new DefaultArtifact(moduleId.toString)

    val request = new ArtifactRequest()
    request.setArtifact(unresolvedArtifact)
    request.setRepositories(Seq(Central))

    val result = system.resolveArtifact(session, request)
    result.getArtifact
  }

  def resolveTransitive(moduleId: ModuleId): Seq[ArtifactResult] = {
    val unresolvedArtifact = new DefaultArtifact(moduleId.toString)
    val dependencyFilter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME)

    val collectRequest = new CollectRequest(new Dependency(unresolvedArtifact, JavaScopes.RUNTIME), Seq(Central))
    val dependencyRequest = new DependencyRequest(collectRequest, dependencyFilter)

    system.resolveDependencies(session, dependencyRequest).getArtifactResults
  }

}
