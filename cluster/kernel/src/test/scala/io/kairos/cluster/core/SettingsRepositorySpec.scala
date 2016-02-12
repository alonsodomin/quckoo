package io.kairos.cluster.core

import java.net.URL

import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import io.kairos.cluster.KairosClusterSettings
import io.kairos.test.TestActorSystem
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * Created by alonsodomin on 12/02/2016.
  */
object SettingsRepositorySpec {
  import SettingsRepository._

  final val TestArtifactRepository = ArtifactRepository("foo", new URL("http://www.example.com"))

}

class SettingsRepositorySpec extends TestKit(TestActorSystem("SettingsRepositorySpec"))
    with ImplicitSender with WordSpecLike with BeforeAndAfterAll with Matchers {

  import SettingsRepository._
  import SettingsRepositorySpec._

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  val defaultSettings = KairosClusterSettings(system)

  "A settings repository" should {
    val settingsRepository = TestActorRef(SettingsRepository.props(defaultSettings))

    "return an empty artifact repositories when it's actually empty" in {
      settingsRepository ! GetArtifactRepositories

      expectMsgType[Seq[ArtifactRepository]] should be (empty)
    }

    "return an event notifying when a repository has been added" in {
      settingsRepository ! AddArtifactRepository(TestArtifactRepository)

      expectMsgType[ArtifactRepositoryAdded].repository should be (TestArtifactRepository)
    }

    "return the previously added artifact repositories" in {
      settingsRepository ! GetArtifactRepositories

      expectMsgType[Seq[ArtifactRepository]] should contain (TestArtifactRepository)
    }

    "remove artifact repositories" in {
      settingsRepository ! RemoveArtifactRepository(TestArtifactRepository)

      expectMsgType[ArtifactRepositoryRemoved].repository should be (TestArtifactRepository)
    }

    "return an empty list of repositories" in {
      settingsRepository ! GetArtifactRepositories

      expectMsgType[Seq[ArtifactRepository]] should be (empty)
    }
  }

}
