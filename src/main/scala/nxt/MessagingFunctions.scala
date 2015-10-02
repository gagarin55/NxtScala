package nxt

import nxt.utils.TransactionTemplates._
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConversions._

object MessagingFunctions {
  def fetchTexts(since: Int, publisherId: Long): Try[Seq[String]] = Try {
    val pms = PrunableMessage.getPrunableMessages(publisherId, 0, Integer.MAX_VALUE)
      .iterator()
      .map(_.toString)
      .toSeq

    //old format fetching below
    val oldMsgs = new TransactionQueryBuilder()
      .withHeightMoreThan(since)
      .withSender(publisherId)
      .withType(1, 0)
      .query()
      .get
      .flatMap(fetchMultiPartMessage)

    oldMsgs ++ pms
  }

  def fetch(id: String): Try[String] = Try (fetchMultiPartMessage(Nxt.getBlockchain.getTransaction(id.toLong)).get)

  //todo: will be not needed after 1.5.x mainnet release
  private def fetchMultiPartMessage(tx: Transaction): Option[String] = {

    def fetchStep(tx: Transaction): Option[String] = {
      val txTry = new TransactionQueryBuilder()
        .withReferenceToTransaction(tx)
        .query()

      val msgTxOpt = txTry match {
        case Success(txAsItr) =>
          txAsItr.find(t => Option(t.getMessage).isDefined)
        case Failure(e) => e.printStackTrace()
          None
      }

      msgTxOpt.map { tx =>
        val s = new String(tx.getMessage.getMessage)
        s + fetchStep(tx).getOrElse("")
      }
    }

    val msgOpt = Option(tx.getMessage).map(_.getMessage).map(ba => new String(ba))

    msgOpt.orElse {
      Option(tx.getMessage)
        .map(_.getMessage)
        .map(b => new String(b))
    }.map(_ + fetchStep(tx).getOrElse(""))
  }

  def publishText(text: String, accountPassphrase: String): Try[Long] =
    sendNonPrunableMultipartMessage(accountPassphrase, text.trim).map(_.getId)
}