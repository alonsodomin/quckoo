package io.quckoo.client.http

import java.util.UUID

import akka.actor.ActorSystem

import io.quckoo.id.{ArtifactId, JobId}
import io.quckoo.serialization.DataBuffer

import org.mockserver.model.{JsonBody, HttpRequest => MockHttpRequest, HttpResponse => MockHttpResponse}
import org.mockserver.verify.VerificationTimes

import org.scalatest.{Matchers, fixture}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by alonsodomin on 20/09/2016.
  */
class AkkaHttpBackendSpec extends fixture.FlatSpec with MockServer with Matchers {
  implicit val actorSystem = ActorSystem("AkkaHttpBackendSpec")

  override protected def afterAll(): Unit = {
    actorSystem.terminate()
    super.afterAll()
  }

  "on send" should "parse error codes correctly in any HTTP method" in { mockServer =>
    val transport = new AkkaHttpBackend("localhost", mockServer.getPort)

    for (method <- HttpMethod.values) {
      val mockHttpRequest = MockHttpRequest.request("/nowhere").withMethod(method.entryName)
      val mockHttpResponse = MockHttpResponse.notFoundResponse()

      mockServer.when(mockHttpRequest).respond(mockHttpResponse)

      val response = Await.result(
        transport.send(HttpRequest(method, "/nowhere", Duration.Inf, Map.empty)),
        Duration.Inf
      )

      response.statusCode shouldBe 404

      mockServer.verify(mockHttpRequest, VerificationTimes.once())
    }
  }

  it should "send JSON body request and parse the JSON output" in { mockServer =>
    val transport = new AkkaHttpBackend("localhost", mockServer.getPort)

    val input = ArtifactId("com.example", "example", "latest")
    val output = JobId(UUID.randomUUID())

    DataBuffer(input).flatMap(in => DataBuffer(output).map(out => (in, out))).foreach {
      case (in, out) =>
        val mockHttpRequest = MockHttpRequest.request("/path").
          withMethod("POST").
          withHeader("Content-Type", "application/json").
          withBody(JsonBody.json(in.asString()))
        val mockHttpResponse = MockHttpResponse.response.withBody(JsonBody.json(out.asString()))

        mockServer.when(mockHttpRequest).respond(mockHttpResponse)

        val headers = Map("Content-Type" -> "application/json")
        val response = Await.result(
          transport.send(HttpRequest(HttpMethod.Post, "/path", Duration.Inf, headers, in)),
          Duration.Inf
        )

        mockServer.verify(mockHttpRequest, VerificationTimes.once())
        response.entity.asString() shouldBe out.asString()
    }
  }

  it should "send Authorization header" in { mockServer =>
    val transport = new AkkaHttpBackend("localhost", mockServer.getPort)

    val mockHttpRequest = MockHttpRequest.request("/path").
      withMethod("POST").
      withHeader("Authorization", "foo")
    val mockHttpResponse = MockHttpResponse.response.withBody("OK!")

    mockServer.when(mockHttpRequest).respond(mockHttpResponse)

    val headers = Map("Authorization" -> "foo")
    val response = Await.result(
      transport.send(HttpRequest(HttpMethod.Post, "/path", Duration.Inf, headers)),
      Duration.Inf
    )

    mockServer.verify(mockHttpRequest, VerificationTimes.once())
    response.entity.asString() shouldBe "OK!"
  }

}
