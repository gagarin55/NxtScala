package nxt.nxtscala.test

import nxt.{Nxt, NxtFunctions}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.util.Random

class NxtScalaSpecification extends FunSuite with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    val propsRes = getClass.getClassLoader.getResource("nxt-default.properties")
    System.setProperty("nxt-default.properties", propsRes.getFile)
    Nxt.init()
  }

  override def afterAll(): Unit = {
    Nxt.shutdown()
  }

  test("balance - non-negative"){
    assert(NxtFunctions.balanceNqt(Random.nextString(50)) >=0)
  }

  test("account - add or get"){
    NxtFunctions.addOrGetAccount(Random.nextString(50))
  }

  test("accountId - always defined"){
    assert(Option(NxtFunctions.accountId(Random.nextString(50))).isDefined)
  }

  test("asset balance - non-negative"){
    assert(NxtFunctions.assetBalance(Random.nextLong())(Random.nextLong()) >=0)
  }
}
