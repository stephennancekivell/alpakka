/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.amqp.scaladsl

import akka.Done
import akka.stream._
import akka.stream.alpakka.amqp._
import akka.stream.scaladsl.{GraphDSL, Merge, Sink, Source}
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import akka.util.ByteString

import scala.concurrent.Promise
import scala.concurrent.duration._

/**
 * Needs a local running AMQP server on the default port with no password.
 */
class AmqpConnectorsSpec extends AmqpSpec {

  override implicit val patienceConfig = PatienceConfig(10.seconds)

  "The AMQP Connectors" should {

    "publish and consume elements through a simple queue again in the same JVM" in {
      //#queue-declaration
      val queueName = "amqp-conn-it-spec-simple-queue-" + System.currentTimeMillis()
      val queueDeclaration = QueueDeclaration(queueName)
      //#queue-declaration

      //#create-sink
      val amqpSink = AmqpSink.simple(
        AmqpSinkSettings(DefaultAmqpConnection).withRoutingKey(queueName).withDeclarations(queueDeclaration)
      )
      //#create-sink

      //#create-source
      val amqpSource = AmqpSource(
        NamedQueueSourceSettings(DefaultAmqpConnection, queueName).withDeclarations(queueDeclaration),
        bufferSize = 10
      )
      //#create-source

      //#run-sink
      val input = Vector("one", "two", "three", "four", "five")
      Source(input).map(s => ByteString(s)).runWith(amqpSink)
      //#run-sink

      //#run-source
      val result = amqpSource.map(_.bytes.utf8String).take(input.size).runWith(Sink.seq)
      //#run-source

      result.futureValue shouldEqual input
    }

    "publish from one source and consume elements with multiple sinks" in {
      val queueName = "amqp-conn-it-spec-work-queues-" + System.currentTimeMillis()
      val queueDeclaration = QueueDeclaration(queueName)
      val amqpSink = AmqpSink.simple(
        AmqpSinkSettings(DefaultAmqpConnection).withRoutingKey(queueName).withDeclarations(queueDeclaration)
      )

      val input = Vector("one", "two", "three", "four", "five")
      Source(input).map(s => ByteString(s)).runWith(amqpSink)

      val mergedSources = Source.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._
        val count = 3
        val merge = b.add(Merge[IncomingMessage](count))
        for (n <- 0 until count) {
          val source = b.add(
              AmqpSource(
                NamedQueueSourceSettings(DefaultAmqpConnection, queueName).withDeclarations(queueDeclaration),
                bufferSize = 1
              ))
          source.out ~> merge.in(n)
        }

        SourceShape(merge.out)
      })

      val result = mergedSources.map(_.bytes.utf8String).take(input.size).runWith(Sink.seq)

      result.futureValue.sorted shouldEqual input.sorted
    }

    "not fail on a fast producer and a slow consumer" in {
      val queueName = "amqp-conn-it-spec-simple-queue-2-" + System.currentTimeMillis()
      val queueDeclaration = QueueDeclaration(queueName)
      val amqpSource = AmqpSource(
        NamedQueueSourceSettings(DefaultAmqpConnection, queueName).withDeclarations(queueDeclaration),
        bufferSize = 2
      )

      val amqpSink = AmqpSink.simple(
        AmqpSinkSettings(DefaultAmqpConnection).withRoutingKey(queueName).withDeclarations(queueDeclaration)
      )

      val publisher = TestPublisher.probe[ByteString]()
      val subscriber = TestSubscriber.probe[IncomingMessage]()
      amqpSink.addAttributes(Attributes.inputBuffer(1, 1)).runWith(Source.fromPublisher(publisher))
      amqpSource.addAttributes(Attributes.inputBuffer(1, 1)).runWith(Sink.fromSubscriber(subscriber))

      // note that this essentially is testing rabbitmq just as much as it tests our sink and source
      publisher.ensureSubscription()
      subscriber.ensureSubscription()

      publisher.expectRequest() shouldEqual 1
      publisher.sendNext(ByteString("one"))

      publisher.expectRequest()
      publisher.sendNext(ByteString("two"))

      publisher.expectRequest()
      publisher.sendNext(ByteString("three"))

      publisher.expectRequest()
      publisher.sendNext(ByteString("four"))

      publisher.expectRequest()
      publisher.sendNext(ByteString("five"))

      subscriber.request(4)
      subscriber.expectNext().bytes.utf8String shouldEqual "one"
      subscriber.expectNext().bytes.utf8String shouldEqual "two"
      subscriber.expectNext().bytes.utf8String shouldEqual "three"
      subscriber.expectNext().bytes.utf8String shouldEqual "four"

      subscriber.request(1)
      subscriber.expectNext().bytes.utf8String shouldEqual "five"

      subscriber.cancel()
      publisher.sendComplete()
    }

    "not ack messages unless they get consumed" in {
      val queueName = "amqp-conn-it-spec-simple-queue-2-" + System.currentTimeMillis()
      val queueDeclaration = QueueDeclaration(queueName)
      val amqpSource = AmqpSource(
        NamedQueueSourceSettings(DefaultAmqpConnection, queueName).withDeclarations(queueDeclaration),
        bufferSize = 10
      )

      val amqpSink = AmqpSink.simple(
        AmqpSinkSettings(DefaultAmqpConnection).withRoutingKey(queueName).withDeclarations(queueDeclaration)
      )

      val publisher = TestPublisher.probe[ByteString]()
      val subscriber = TestSubscriber.probe[IncomingMessage]()
      amqpSink.addAttributes(Attributes.inputBuffer(1, 1)).runWith(Source.fromPublisher(publisher))
      amqpSource.addAttributes(Attributes.inputBuffer(1, 1)).runWith(Sink.fromSubscriber(subscriber))

      // note that this essentially is testing rabbitmq just as much as it tests our sink and source
      publisher.ensureSubscription()
      subscriber.ensureSubscription()

      publisher.expectRequest() shouldEqual 1
      publisher.sendNext(ByteString("one"))

      publisher.expectRequest()
      publisher.sendNext(ByteString("two"))

      publisher.expectRequest()
      publisher.sendNext(ByteString("three"))

      publisher.expectRequest()
      publisher.sendNext(ByteString("four"))

      publisher.expectRequest()
      publisher.sendNext(ByteString("five"))

      // this should lead to all five being fetched into the buffer
      // but we just consume one before we cancel
      subscriber.request(1)
      subscriber.expectNext().bytes.utf8String shouldEqual "one"

      subscriber.cancel()
      publisher.sendComplete()

      val subscriber2 = TestSubscriber.probe[IncomingMessage]()
      amqpSource.addAttributes(Attributes.inputBuffer(1, 1)).runWith(Sink.fromSubscriber(subscriber2))

      subscriber2.ensureSubscription()
      subscriber2.request(4)
      subscriber2.expectNext().bytes.utf8String shouldEqual "two"
      subscriber2.expectNext().bytes.utf8String shouldEqual "three"
      subscriber2.expectNext().bytes.utf8String shouldEqual "four"
      subscriber2.expectNext().bytes.utf8String shouldEqual "five"

      subscriber2.cancel()
    }

    "pub-sub from one source with multiple sinks" in {
      // with pubsub we arrange one exchange which the sink writes to
      // and then one queue for each source which subscribes to the
      // exchange - all this described by the declarations

      //#exchange-declaration
      val exchangeName = "amqp-conn-it-spec-pub-sub-" + System.currentTimeMillis()
      val exchangeDeclaration = ExchangeDeclaration(exchangeName, "fanout")
      //#exchange-declaration

      //#create-exchange-sink
      val amqpSink = AmqpSink.simple(
        AmqpSinkSettings(DefaultAmqpConnection).withExchange(exchangeName).withDeclarations(exchangeDeclaration)
      )
      //#create-exchange-sink

      //#create-exchange-source
      val fanoutSize = 4

      val mergedSources = (0 until fanoutSize).foldLeft(Source.empty[(Int, String)]) {
        case (source, fanoutBranch) =>
          source.merge(
            AmqpSource(
              TemporaryQueueSourceSettings(
                DefaultAmqpConnection,
                exchangeName
              ).withDeclarations(exchangeDeclaration),
              bufferSize = 1
            ).map(msg => (fanoutBranch, msg.bytes.utf8String))
          )
      }
      //#create-exchange-source

      val completion = Promise[Done]
      mergedSources.runWith(Sink.fold(Set.empty[Int]) {
        case (seen, (branch, element)) =>
          if (seen.size == fanoutSize) completion.trySuccess(Done)
          seen + branch
      })

      import system.dispatcher
      system.scheduler.scheduleOnce(5.seconds)(
          completion.tryFailure(new Error("Did not get at least one element from every fanout branch")))

      Source.repeat("stuff").map(s => ByteString(s)).runWith(amqpSink)

      completion.future.futureValue shouldBe Done
    }
  }
}
