/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.gitter

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.FreeSpec
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl._
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

class GitterSpec extends FreeSpec with ScalaFutures {

  //#init-mat
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  //#init-mat

  implicit val defaultPatience =
    PatienceConfig(timeout = 600.seconds, interval = 2.seconds)

  "foo" - {

    "does in " in {
      val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
        Http().outgoingConnectionHttps("stream.gitter.im")

        val bearer: String = ???

      val headers = List(
        RawHeader("Accept", "application/json"),
        RawHeader("Authorization", "Bearer "+bearer)
      )

      val url = "/v1/rooms/54f9e9a215522ed4b3dce824/chatMessages"

      val result = Source
        .single(HttpRequest(uri = url, headers = headers))
        .via(connectionFlow)
        .flatMapConcat { response =>
          response.entity.dataBytes
        }
        .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 128 * 1024))
        .map(_.decodeString("utf-8"))
        .take(10)
        .runWith(Sink.foreach { str =>
          println("str " + str)
        })

      result.futureValue

      assert(true)
    }
  }
}
