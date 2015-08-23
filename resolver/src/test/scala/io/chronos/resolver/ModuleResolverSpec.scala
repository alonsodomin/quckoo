package io.chronos.resolver

import java.net.URL

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import io.chronos.id.ModuleId
import io.chronos.protocol.ResolutionFailed
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
 * Created by domingueza on 23/08/15.
 */
class ModuleResolverSpec extends TestKit(ActorSystem("ModuleResolverSpec")) with ImplicitSender
  with WordSpecLike with BeforeAndAfterAll with Matchers with MockFactory {

  import ModuleResolver._

  final val TestModuleId = ModuleId("com.example", "foo", "latest")

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A module resolver" should {
    val mockResolve = mockFunction[ModuleId, Boolean, Either[ResolutionFailed, JobPackage]]
    val moduleResolver = TestActorRef(ModuleResolver.props(mockResolve))

    "return the job package on successful resolution of dependencies" in {
      val jobPackage = JobPackage(TestModuleId, Seq(new URL("http://www.google.com")))

      mockResolve expects (TestModuleId, false) returning Right(jobPackage)

      moduleResolver ! ResolveModule(TestModuleId, download = false)

      expectMsg(jobPackage)
    }

    "return resolution failed error if it can't resolve dependencies" in {
      val expectedResolutionFailed = ResolutionFailed(Seq("com.foo.bar"))

      mockResolve expects (TestModuleId, false) returning Left(expectedResolutionFailed)

      moduleResolver ! ResolveModule(TestModuleId, download = false)

      expectMsg(expectedResolutionFailed)
    }

    "return an error message if an exception happens" in {
      val expectedException = new RuntimeException("TEST EXCEPTION")

      mockResolve expects (TestModuleId, false) throwing expectedException

      moduleResolver ! ResolveModule(TestModuleId, download = false)

      expectMsgType[ErrorResolvingModule].cause should be (expectedException)
    }
  }

}
