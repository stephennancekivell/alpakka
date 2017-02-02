# MongoDB Connector

The MongoDB connector provides a way to provide the result of a MongoDB query as a stream of documents.

## Artifacts

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-mongodb" % "$version$"
    ```
    @@@

Maven
:   @@@vars
    ```xml
    <dependency>
      <groupId>com.lightbend.akka</groupId>
      <artifactId>akka-stream-alpakka-mongodb_$scala.binaryVersion$</artifactId>
      <version>$version$</version>
    </dependency>
    ```
    @@@

Gradle
:   @@@vars
    ```gradle
    dependencies {
      compile group: "com.lightbend.akka", name: "akka-stream-alpakka-mongodb_$scala.binaryVersion$", version: "$version$"
    }
    ```
    @@@

## Usage

Sources provided by this connector need a prepared session to communicate with MongoDB server. First, lets initialize a MongoDB connection.

Scala
: @@snip (../../../../mongodb/src/test/scala/akka/stream/alpakka/mongodb/MongoSourceSpec.scala) { #init-connection }

We will also need an @scaladoc[ActorSystem](akka.actor.ActorSystem) and an @scaladoc[ActorMaterializer](akka.stream.ActorMaterializer).

Scala
: @@snip (../../../../mongodb/src/test/scala/akka/stream/alpakka/mongodb/MongoSourceSpec.scala) { #init-mat }

This is all preparation that we are going to need.

### Source Usage

Let's create a source from a MongoDB collection observable, which can optionally take a filter.

Scala
: @@snip (../../../../mongodb/src/test/scala/akka/stream/alpakka/mongodb/MongoSourceSpec.scala) { #create-source }

And finally create the source using any method from the @scaladoc[CassandraSource](akka.stream.alpakka.cassandra.CassandraSource$) factory and run it.

Scala
: @@snip (../../../../mongodb/src/test/scala/akka/stream/alpakka/mongodb/MongoSourceSpec.scala) { #run-source }

Here we used a basic sink to complete the stream by collecting all of the stream elements to a collection. The power of streams comes from building larger data pipelines which leverage backpressure to ensure efficient flow control. Feel free to edit the example code and build @extref[more advanced stream topologies](akka-docs:scala/stream/stream-introduction).


### Running the example code

The code in this guide is part of runnable tests of this project. You are welcome to edit the code and run it in sbt.

> Test code requires Cassandra server running in the background. You can start one quickly using docker:
>
> `docker run --rm mongo`

Scala
:   ```
    sbt
    > mongodb/test
    ```