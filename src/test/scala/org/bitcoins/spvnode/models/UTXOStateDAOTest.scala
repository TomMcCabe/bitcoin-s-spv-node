package org.bitcoins.spvnode.models

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.gen.{CryptoGenerators, NumberGenerator, TransactionGenerators}
import org.bitcoins.spvnode.constant.TestConstants
import org.bitcoins.spvnode.modelsd.BlockHeaderTable
import org.bitcoins.spvnode.utxo.UTXOState
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import slick.driver.PostgresDriver.api._

/**
  * Created by chris on 9/24/16.
  */
class UTXOStateDAOTest extends TestKit(ActorSystem("BlockHeaderDAOTest")) with ImplicitSender
  with FlatSpecLike with MustMatchers with BeforeAndAfter with BeforeAndAfterAll {

  val table = TableQuery[UTXOStateTable]
  val database: Database = TestConstants.database
  def utxoState = {
    val output = TransactionGenerators.outputs.sample.get
    val vout = NumberGenerator.uInt32s.sample.get
    val txId = CryptoGenerators.doubleSha256Digest.sample.get
    val blockHash = CryptoGenerators.doubleSha256Digest.sample.get
    val isSpent = (scala.util.Random.nextInt() % 2).abs == 1
    UTXOState(output,vout,txId,blockHash,isSpent)
  }
  before {
    //Awaits need to be used to make sure this is fully executed before the next test case starts
    //TODO: Figure out a way to make this asynchronous
    Await.result(database.run(table.schema.create), 10.seconds)
  }

  "UTXOStateDAO" must "create a utxo to track in the database" in {
    val (utxoStateDAO, probe) = utxoStateDAORef
    val u = utxoState
    val createMsg = UTXOStateDAO.Create(u)
    utxoStateDAO ! createMsg
    val created = probe.expectMsgType[UTXOStateDAO.Created]

    //make sure we can read it
    val read = UTXOStateDAO.Read(created.uTXOState.id.get)
    utxoStateDAO ! read

    val readMsg = probe.expectMsgType[UTXOStateDAO.ReadReply]
    readMsg.utxoState.get must be (created.uTXOState)
  }

  it must "find all outputs by a given set of txids" in {
    val (utxoStateDAO, probe) = utxoStateDAORef
    val u1 = utxoState
    val u2 = utxoState

    val createMsg1 = UTXOStateDAO.Create(u1)
    utxoStateDAO ! createMsg1
    val created1 = probe.expectMsgType[UTXOStateDAO.Created]

    val createMsg2 = UTXOStateDAO.Create(u2)
    utxoStateDAO ! createMsg2
    val created2 = probe.expectMsgType[UTXOStateDAO.Created]

    val txids = Seq(u1.txId, u2.txId)
    utxoStateDAO ! UTXOStateDAO.FindTxIds(txids)

    val foundTxIds = probe.expectMsgType[UTXOStateDAO.FindTxIdsReply]
    val expectedUtxoStates = Seq(created1,created2).map(_.uTXOState)
    foundTxIds.utxoStates must be (expectedUtxoStates)
  }

  after {
    //Awaits need to be used to make sure this is fully executed before the next test case starts
    //TODO: Figure out a way to make this asynchronous
    Await.result(database.run(table.schema.drop),10.seconds)
  }

  override def afterAll = {
    database.close()
    TestKit.shutdownActorSystem(system)
  }

  private def utxoStateDAORef: (TestActorRef[BlockHeaderDAO], TestProbe) = {
    val probe = TestProbe()
    val utxoStateDAO: TestActorRef[BlockHeaderDAO] = TestActorRef(UTXOStateDAO.props(TestConstants),probe.ref)
    (utxoStateDAO,probe)
  }
}
