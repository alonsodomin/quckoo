package io.kairos.resolver.ivy

import java.net.URL

import akka.actor.ActorSystem
import io.kairos.id.ArtifactId
import io.kairos.protocol._
import io.kairos.resolver._
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor.{Configuration, DefaultDependencyDescriptor, DefaultModuleDescriptor, ModuleDescriptor}
import org.apache.ivy.core.module.id.{ModuleRevisionId => IvyModuleId}
import org.apache.ivy.core.report.{ArtifactDownloadReport, DownloadStatus, ResolveReport}
import org.apache.ivy.core.resolve.ResolveOptions

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._
import scalaz._

/**
 * Created by aalonsodominguez on 17/07/15.
 */
object IvyResolve {

  private final val DefaultConfName = "default"
  private final val CompileConfName = "compile"
  private final val RuntimeConfName = "runtime"

  private final val Configurations = Array(DefaultConfName, CompileConfName, RuntimeConfName)

  def apply(system: ActorSystem): IvyResolve = {
    val settings = IvyConfiguration(system.settings.config)
    new IvyResolve(settings)
  }

}

class IvyResolve(config: IvyConfiguration) extends Resolve {

  import IvyResolve._

  private lazy val ivy = Ivy.newInstance(config)

  def apply(artifactId: ArtifactId, download: Boolean)(implicit ec: ExecutionContext): Future[ResolutionResult] = Future {

    def unresolvedDependencies(report: ResolveReport): ValidationNel[Fault, ResolveReport] = {
      report.getUnresolvedDependencies.map(_.getId).map { moduleId =>
        val unresolvedId = ArtifactId(moduleId.getOrganisation, moduleId.getName, moduleId.getRevision)
        UnresolvedDependency(unresolvedId).failureNel[ResolveReport]
      }.foldLeft(report.successNel[Fault])((a, b) => (a |@| b) { case (_, r) => r })
    }

    def downloadFailed(report: ResolveReport): ValidationNel[Fault, ResolveReport] = {
      report.getArtifactsReports(DownloadStatus.FAILED, true).map { artifactReport =>
        DownloadFailed(artifactReport.getName).failureNel[ResolveReport]
      }.foldLeft(report.successNel[Fault])((a, b) => (a |@| b) { case (_, r) => r })
    }

    def artifactLocations(artifactReports: Seq[ArtifactDownloadReport]): Seq[URL] = {
      for (report <- artifactReports) yield {
        Option(report.getUnpackedLocalFile).
          orElse(Option(report.getLocalFile)) match {
          case Some(file) => Right(file)
          case None       => Left(report.getArtifact.getUrl)
        }
      } fold (identity, _.toURI.toURL)
    }

    val moduleDescriptor = newCallerInstance(artifactId)
    val resolveOptions = new ResolveOptions().
      setTransitive(true).
      setValidate(true).
      setDownload(download).
      setOutputReport(false).
      setConfs(Array(RuntimeConfName))

    val resolveReport = ivy.resolve(moduleDescriptor, resolveOptions)

    (unresolvedDependencies(resolveReport) |@| downloadFailed(resolveReport)) { (_, r) =>
      Artifact(artifactId, artifactLocations(r.getAllArtifactsReports))
    }

    /*if (resolveReport.hasError) {
      UnresolvedDependencies(resolveReport.getUnresolvedDependencies.map(_.getModuleId.toString)).failureNel[Artifact]
    } else {
      Artifact(artifactId, artifactLocations(resolveReport.getAllArtifactsReports)).successNel[BaseError]
    }*/
  }

  private def newCallerInstance(artifactId: ArtifactId): ModuleDescriptor = {
    val moduleRevisionId: IvyModuleId = IvyModuleId.newInstance(
      artifactId.group, artifactId.artifact, artifactId.version
    )

    val descriptor = new DefaultModuleDescriptor(IvyModuleId.newInstance(
      moduleRevisionId.getOrganisation, moduleRevisionId.getName + "-job", "working"), "execution", null, true
    )
    Configurations.foreach(c => descriptor.addConfiguration(new Configuration(c)))
    descriptor.setLastModified(System.currentTimeMillis)

    val dependencyDescriptor = new DefaultDependencyDescriptor(descriptor, moduleRevisionId, true, true, true)
    Configurations.foreach(c => dependencyDescriptor.addDependencyConfiguration(c, c))
    descriptor.addDependency(dependencyDescriptor)

    descriptor
  }

}
