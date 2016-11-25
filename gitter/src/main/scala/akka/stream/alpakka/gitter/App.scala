/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.gitter

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.{ Failure, Success }

object AppIt extends scala.App {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    Http().outgoingConnectionHttps("stream.gitter.im")

    val bearer: String = ???

  val headers = List(
    RawHeader("Accept", "application/json"),
    RawHeader("Authorization", "Bearer "+bearer)
  )

  val url = "/v1/rooms/545a9787db8155e6700d192a/chatMessages"
  //val url = "/v1/rooms/546fd572db8155e6700d6eaf/chatMessages" // free code camp

  val s = Source
    .single(HttpRequest(uri = url, headers = headers))
    .via(connectionFlow)
    .flatMapConcat { response =>
      println("flatMapConcat " + response.status)

      response.entity.dataBytes
    }
    .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 2560))
    .map({ r =>
      val decoded = r.decodeString("utf-8")
      println("decodeded " + decoded)
      decoded
    })
    .runWith(Sink.foreach { str =>
      println("str " + str)
    })

}
