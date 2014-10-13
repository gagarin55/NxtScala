package nxt.utils

import nxt._
import nxt.crypto.{EncryptedData, Crypto}
import scala.util.Try
import nxt.Appendix.{EncryptToSelfMessage, EncryptedMessage, Message}
import org.joda.time.DateTime


case class Messages(plainMessage: Option[String], encryptedMessage: Option[String], encryptedMessageToSelf: Option[String])

object Messages {
  def noMessages = new Messages(None, None, None)

  def plainOnly(msg: String) = new Messages(Some(msg), None, None)

  def encryptedOnly(msg: String) = new Messages(None, Some(msg), None)

  def encryptedToSelfOnly(msg: String) = new Messages(None, None, Some(msg))
}


object TxUtils {
  val Fee = Constants.ONE_NXT
  val defaultDeadline: Short = 1440

  def issueTx(phrase: String, attachment: Attachment, amount: Long,
              recipientOpt: Option[Long], rcpPubKeyOpt: Option[Array[Byte]],
              messages: Messages): Try[Transaction] = Try {

    val pubKey = Crypto.getPublicKey(phrase)


    val fee = attachment match {
      case _: Attachment.ColoredCoinsAssetIssuance => NxtFunctions.toNqt(1001)
      case _: Attachment.MessagingPollCreation => NxtFunctions.toNqt(11)
      case _ => Fee
    }

    println(s"Going to issue transaction with attachment to $recipientOpt : ${attachment.getJSONObject}, fee is $fee")

    val tb = Nxt.getTransactionProcessor.newTransactionBuilder(pubKey, amount, fee, defaultDeadline, attachment)
    recipientOpt.map(rcp => tb.recipientId(rcp))
    rcpPubKeyOpt.map(pubKey => tb.publicKeyAnnouncement(NxtFunctions.announcement(pubKey)))
    messages.plainMessage.map(message => tb.message(new Message(message)))
    messages.encryptedMessage.flatMap{message =>
      val privKey = Crypto.getPrivateKey(phrase)
      val theirPubOpt = rcpPubKeyOpt.orElse(recipientOpt.map(id=> Account.getAccount(id).getPublicKey))

      theirPubOpt map { theirPub =>
        val ed = EncryptedData.encrypt(message.getBytes, privKey, theirPub)
        tb.encryptedMessage(new EncryptedMessage(ed, true)) //todo: only text messages for now
      }
    }

    messages.encryptedMessageToSelf.map{message =>
      recipientOpt.map {rcpId =>
        val ed = Account.getAccount(rcpId).encryptTo(message.getBytes, phrase)
        tb.encryptToSelfMessage(new EncryptToSelfMessage(ed, true)) //todo: only text messages for now
      }
    }

    val tx = tb.build()

    tx.sign(phrase)
    Nxt.getTransactionProcessor.broadcast(tx)
    tx
  }

  def issueTx(phrase: String, attachment: Attachment, amount: Long, recipientOpt: Option[Long]): Try[Transaction] =
    issueTx(phrase, attachment, amount, recipientOpt, None, Messages.noMessages)

  def issueTx(phrase: String, attachment: Attachment): Try[Transaction] = issueTx(phrase, attachment, 0, None)


  def sendMoney(phrase: String, amount: Long, recipient: Long) =
    issueTx(phrase, Attachment.ORDINARY_PAYMENT, amount, Some(recipient))

  def sendMoney(phrase: String, amount: Long, recipient: Long, recipientPubKey:Array[Byte]) =
    issueTx(phrase, Attachment.ORDINARY_PAYMENT, amount, Some(recipient), Some(recipientPubKey), Messages.noMessages)

  def sendMessage(phrase: String, text: String, recipient: Option[Long]) =
    issueTx(phrase, Attachment.ARBITRARY_MESSAGE, 0, recipient, None, Messages.plainOnly(text))

  def sendMessage(phrase: String, text: String, recipient: Long) =
    issueTx(phrase, Attachment.ARBITRARY_MESSAGE, 0, Some(recipient), None, Messages.plainOnly(text))

  def sendMessage(phrase: String, text: String) =
    issueTx(phrase, Attachment.ARBITRARY_MESSAGE, 0, None, None, Messages.plainOnly(text))

  def publicKeyAnnouncement(phrase: String, senderPhrase:String) = {
    val pk = Crypto.getPublicKey(phrase)
    val accId = Account.getId(pk)
    issueTx(senderPhrase, Attachment.ORDINARY_PAYMENT, 1, Some(accId), Some(pk), Messages.noMessages)
  }

  def checkThenFixPubKey(phrase: String, senderPhrase:String) = {
    if (Option(NxtFunctions.addOrGetAccount(phrase).getPublicKey).isEmpty) {
      publicKeyAnnouncement(phrase, senderPhrase)
    }
  }
}


object TxSeqUtils {
  def withTextMessage(txs: Iterable[Transaction]): Iterable[Transaction] = txs flatMap {
    tx =>
      Option(tx.getMessage).flatMap(msgAppendix => if (!msgAppendix.isText) None else Some(tx))
  }

  def betweenTimestamps(txs: Iterable[Transaction], startTime:DateTime, endTime:DateTime) = txs.filter{tx =>
    val timestamp = tx.getTimestamp*1000L + Constants.EPOCH_BEGINNING
    timestamp <= endTime.getMillis && timestamp >= startTime.getMillis
  }
}