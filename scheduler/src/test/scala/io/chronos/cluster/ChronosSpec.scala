package io.chronos.cluster

import java.time.{Clock, Instant, ZoneId}

import akka.actor.Props
import akka.cluster.sharding.ClusterShardingSettings
import akka.testkit._
import io.chronos.protocol._
import io.chronos.scheduler.TestActorSystem
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

/**
 * Created by domingueza on 26/08/15.
 */
object ChronosSpec {

  final val FixedInstant = Instant.ofEpochMilli(2983928L)
  final val ZoneUTC = ZoneId.of("UTC")

}

class ChronosSpec extends TestKit(TestActorSystem("ChronosSpec")) with ImplicitSender
  with FlatSpecLike with BeforeAndAfterAll with Matchers {

  import ChronosSpec._

  implicit val clock = Clock.fixed(FixedInstant, ZoneUTC)

  val shardSettings = ClusterShardingSettings(system)

  val registryProbe = TestProbe()
  val resolverProbe = TestProbe()
  val queueProbe = TestProbe()

  val chronosProps: Props = Chronos.props(shardSettings,
    TestActors.forwardActorProps(resolverProbe.ref),
    TestActors.forwardActorProps(queueProbe.ref)
  ) { TestActors.forwardActorProps }
  val chronos = TestActorRef(chronosProps)

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "Chronos" should "reply with a connected message when client requests to" in {
    chronos ! Connect

    expectMsg(Connected)
  }

  it should "reply with a disconnected message when client asks for it" in {
    chronos ! Disconnect

    expectMsg(Disconnected)
  }

  it should "reply with the cluster status when client asks for it" in {
    chronos ! GetClusterStatus

    expectMsgType[ClusterStatus]
  }

}
