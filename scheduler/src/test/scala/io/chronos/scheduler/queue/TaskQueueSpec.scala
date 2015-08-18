package io.chronos.scheduler.queue

import java.util.UUID

import akka.actor.Address
import akka.pattern._
import akka.testkit._
import io.chronos.cluster.WorkerProtocol.RegisterWorker
import io.chronos.scheduler.TestActorSystem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 18/08/15.
 */
object TaskQueueSpec {

  final val TestMaxTaskTimeout = 5 seconds

}

class TaskQueueSpec extends TestKit(TestActorSystem("TaskQueueSpec")) with ImplicitSender with DefaultTimeout
  with FlatSpecLike with BeforeAndAfterAll with ScalaFutures with Matchers {

  import TaskQueue._
  import TaskQueueSpec._

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  val taskQueue = TestActorRef(TaskQueue.props(TestMaxTaskTimeout))

  "A TaskQueue" should "register workers and return their address" in {
    val workerId = UUID.randomUUID()
    val workerProbe = TestProbe("workerProbe")

    taskQueue.tell(RegisterWorker(workerId), workerProbe.ref)

    whenReady((taskQueue ? GetWorkers).mapTo[Seq[Address]]) { addresses =>
      addresses should contain (workerProbe.ref.path.address)
    }
  }

}
