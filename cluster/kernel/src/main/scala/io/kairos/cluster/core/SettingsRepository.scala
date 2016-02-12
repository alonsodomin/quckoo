package io.kairos.cluster.core

import java.net.URL

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import io.kairos.cluster.KairosClusterSettings

/**
  * Created by alonsodomin on 12/02/2016.
  */
object SettingsRepository {

  final val PersistenceId = "KairosSettings"

  sealed trait ArtifactRepositoryType
  object ArtifactRepositoryType {
    case object Maven extends ArtifactRepositoryType
  }

  case class ArtifactRepository(name: String, url: URL,
    repositoryType: ArtifactRepositoryType = ArtifactRepositoryType.Maven
  )

  sealed trait SettingsEvent
  sealed trait SettingsCommand

  case class AddArtifactRepository(repository: ArtifactRepository) extends SettingsCommand
  case class RemoveArtifactRepository(repository: ArtifactRepository) extends SettingsCommand
  case object GetArtifactRepositories extends SettingsCommand

  case class ArtifactRepositoryAdded(repository: ArtifactRepository) extends SettingsEvent
  case class ArtifactRepositoryRemoved(repository: ArtifactRepository) extends SettingsEvent

  private object SettingsStore {
    def empty: SettingsStore = SettingsStore(Vector.empty)
  }
  private case class SettingsStore private (
      artifactRepositories: Vector[ArtifactRepository]
  ) {

    def update(event: SettingsEvent) = event match {
      case ArtifactRepositoryAdded(repo) =>
        copy(artifactRepositories = artifactRepositories :+ repo)

      case ArtifactRepositoryRemoved(repo) =>
        copy(artifactRepositories = artifactRepositories.filterNot(_ == repo))
    }

  }

  def props(clusterSettings: KairosClusterSettings): Props =
    Props(classOf[SettingsRepository], clusterSettings)

}

class SettingsRepository(clusterSettings: KairosClusterSettings)
    extends PersistentActor with ActorLogging {
  import SettingsRepository._

  private[this] var store = SettingsStore.empty

  override def persistenceId: String = PersistenceId

  override def receiveRecover: Receive = {
    case event: SettingsEvent =>
      store = store.update(event)
  }

  override def receiveCommand: Receive = {
    case AddArtifactRepository(repo) =>
      persist(ArtifactRepositoryAdded(repo)) { event =>
        store = store.update(event)
        sender() ! event
      }

    case RemoveArtifactRepository(repo) =>
      persist(ArtifactRepositoryRemoved(repo)) { event =>
        store = store.update(event)
        sender() ! event
      }

    case GetArtifactRepositories =>
      sender() ! store.artifactRepositories
  }

}
