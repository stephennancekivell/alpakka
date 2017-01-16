/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.dynamodb

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.dynamodb.impl.{ DynamoClientImpl, DynamoSettings }
import akka.stream.alpakka.dynamodb.scaladsl.DynamoClient
import akka.testkit.{ SocketUtil, TestKit }
import org.scalatest.{ AsyncWordSpecLike, BeforeAndAfterAll, Matchers }

class TableSpec extends TestKit(ActorSystem("TableSpec")) with AsyncWordSpecLike with Matchers with BeforeAndAfterAll {

  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val settings = DynamoSettings(system).copy(port = SocketUtil.temporaryServerAddress().getPort)
  val localDynamo = new LocalDynamo(settings)

  override def beforeAll(): Unit = localDynamo.start()

  override def afterAll(): Unit = {
    localDynamo.stop()
    system.terminate()
  }

  val client = DynamoClient(settings)

  "DynamoDB Client" should {

    import TableSpecOps._
    import akka.stream.alpakka.dynamodb.scaladsl.DynamoImplicits._

    "1) create table" in {
      client.single(createTableRequest).map(_.getTableDescription.getTableName shouldEqual tableName)
    }

    "2) list tables" in {
      client.single(listTablesRequest).map(_.getTableNames.size shouldEqual 1)
    }

    "3) describe table" in {
      client.single(describeTableRequest).map(_.getTable.getTableName shouldEqual tableName)
    }

    "4) update table" in {
      client
        .single(describeTableRequest)
        .map(_.getTable.getProvisionedThroughput.getWriteCapacityUnits shouldEqual 10L)
        .flatMap(_ => client.single(updateTableRequest))
        .map(_.getTableDescription.getProvisionedThroughput.getWriteCapacityUnits shouldEqual newMaxLimit)
    }

    "5) delete table" in {
      client
        .single(deleteTableRequest)
        .flatMap(_ => client.single(listTablesRequest))
        .map(_.getTableNames.size shouldEqual 0)
    }

  }

}
