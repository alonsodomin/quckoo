package io.chronos.resolver

import java.net.URL

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import io.chronos.id.ModuleId
import io.chronos.protocol.ResolutionFailed
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

/**
 * Created by domingueza on 23/08/15.
 */
class ResolverSpec extends TestKit(ActorSystem("ResolverSpec")) with ImplicitSender
  with FlatSpecLike with BeforeAndAfterAll with Matchers with MockFactory {

  import Resolver._

  final val TestModuleId = ModuleId("com.example", "foo", "latest")
  val mockResolve = mockFunction[ModuleId, Boolean, Either[ResolutionFailed, JobPackage]]
  val moduleResolver = TestActorRef(Resolver.props(mockResolve))

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A resolver" should "return the job package when it's valid" in {
    val jobPackage = JobPackage(TestModuleId, Seq(new URL("http://www.google.com")))

    mockResolve expects (TestModuleId, false) returning Right(jobPackage)

    moduleResolver ! Validate(TestModuleId)

    expectMsg(jobPackage)
  }

  it should "return resolution failed error if the package is not valid" in {
    val expectedResolutionFailed = ResolutionFailed(Seq("com.foo.bar"))

    mockResolve expects (TestModuleId, false) returning Left(expectedResolutionFailed)

    moduleResolver ! Validate(TestModuleId)

    expectMsg(expectedResolutionFailed)
  }

  it should "return an error message if an exception happens when validating" in {
    val expectedException = new RuntimeException("TEST EXCEPTION")

    mockResolve expects (TestModuleId, false) throwing expectedException

    moduleResolver ! Validate(TestModuleId)

    expectMsgType[ErrorResolvingModule].cause should be (expectedException)
  }

  it should "return the job package on successful resolution of dependencies" in {
    val jobPackage = JobPackage(TestModuleId, Seq(new URL("http://www.google.com")))

    mockResolve expects (TestModuleId, true) returning Right(jobPackage)

    moduleResolver ! Resolve(TestModuleId)

    expectMsg(jobPackage)
  }

  it should "return resolution failed error if it can't resolve dependencies" in {
    val expectedResolutionFailed = ResolutionFailed(Seq("com.foo.bar"))

    mockResolve expects (TestModuleId, true) returning Left(expectedResolutionFailed)

    moduleResolver ! Resolve(TestModuleId)

    expectMsg(expectedResolutionFailed)
  }

  it should "return an error message if an exception happens" in {
    val expectedException = new RuntimeException("TEST EXCEPTION")

    mockResolve expects (TestModuleId, true) throwing expectedException

    moduleResolver ! Resolve(TestModuleId)

    expectMsgType[ErrorResolvingModule].cause should be (expectedException)
  }

}
