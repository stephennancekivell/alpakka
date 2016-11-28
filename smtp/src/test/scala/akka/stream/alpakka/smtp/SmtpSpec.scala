/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.smtp

import java.util.{ Date, Properties }
import javax.mail.internet.{ InternetAddress, MimeMessage }

import org.scalatest._
import akka.stream.scaladsl._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

class SmtpSpec extends FreeSpec {
  //#init-mat
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  //#init-mat

  "foo" -> {
    "should " in {

      import javax.mail._

      val props = new Properties()
      props.put("mail.smtp.host", "localhost")
      props.put("mail.smtp.port", "2525")
      val session = Session.getInstance(props, null)

      try {
        val msg = new MimeMessage(session)
        msg.setFrom(new InternetAddress("me@example.com"))
        msg.setRecipients(Message.RecipientType.TO, "you@example.com")
        msg.setSubject("JavaMail hello world example")
        msg.setSentDate(new Date())
        msg.setText("Hello, world!\n")

        Transport.send(msg)
      } catch {
        case ex: MessagingException =>
          ex.printStackTrace()
      }

      println("hello test")

      Source(0 to 10).map { i =>
        val msg = new MimeMessage(session)
        msg.setFrom(new InternetAddress("me@example.com"))
        msg.setRecipients(Message.RecipientType.TO, "you@example.com")
        msg.setSubject("JavaMail hello world example")
        msg.setSentDate(new Date())
        msg.setText("Hello, world!\n")
        msg
      }.runWith(Sink.foreach { msg =>
        Transport.send(msg)
      })

      assert(true)
    }
  }
}
