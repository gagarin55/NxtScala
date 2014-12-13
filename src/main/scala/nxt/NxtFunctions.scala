package nxt

import nxt.crypto.Crypto
import nxt.Appendix.PublicKeyAnnouncement
import nxt.Db

object NxtFunctions {

  def withinDbTransaction[T](fn: => T):T={
    Db.db.beginTransaction()
    val t = fn
    Db.db.endTransaction()
    t
  }

  def currentHeight = Nxt.getBlockchain.getHeight
  def heightOfBlock(id:Long):Option[Int] = Option(BlockDb.findBlock(id)).map(_.getHeight)
  def lastFeederHeight = Nxt.getBlockchainProcessor.getLastBlockchainFeederHeight

  def balanceNqt(id:Long):Long = Option(Account.getAccount(id)).map(_.getBalanceNQT).getOrElse(0)
  def balanceNqt(phrase:String):Long = balanceNqt(accountId(phrase))
  def balancesNqt(phrases:String*):Seq[Long] = phrases.map(balanceNqt)
  def totalNqt(phrases:String*):Long = phrases.map(balanceNqt).sum

  def toNqt(nxt:Long) = nxt * Constants.ONE_NXT
  def toNxt(nqt:Long) = nqt / Constants.ONE_NXT

  def balanceNxt(id:Long):Long = toNxt(balanceNqt(id))
  def balanceNxt(phrase:String):Long = toNxt(balanceNqt(phrase))
  def totalNxt(phrases:String*):Long = toNxt(totalNqt(phrases:_*))
  def balancesNxt(phrases:String*):Seq[Long] = phrases.map(balanceNqt).map(toNxt)

  def getAssetBalance(accountId:Long, assetId:Long):Long =
    Option(Account.getAccount(accountId)).map(_.getAssetBalanceQNT(assetId)).getOrElse(0L)

  //as class constructor has package-wide visibility
  def announcement(pubKey:Array[Byte]) = new PublicKeyAnnouncement(pubKey)

  //as method Account.addOrGetAccount has package-wide visibility
  def addOrGetAccount(phrase:String):Account = addOrGetAccount(Account.getId(Crypto.getPublicKey(phrase)))
  def addOrGetAccount(accountId:Long):Account = withinDbTransaction(Account.addOrGetAccount(accountId))

  def transactionById(id:Long) = Option(TransactionDb.findTransaction(id))

  def accountId(phrase:String):Long = Account.getId(Crypto.getPublicKey(phrase))
  def accountIds(phrases:Seq[String]):Seq[Long] = phrases map accountId

  def generateBlock(phrase:String) =
    Nxt.getBlockchainProcessor.asInstanceOf[BlockchainProcessorImpl].generateBlock(phrase, getEpochTime)

  /**
   * Return current time in Nxt Epoch Format(number of seconds from genesis block). It was a part of
   * nxt.util.Convert before 1.3.0
   * @return number of seconds since genesis
   */
  def getEpochTime: Int =
    ((System.currentTimeMillis - Constants.EPOCH_BEGINNING + 500) / 1000).toInt


  /**
    * Pop off blocks and then remove unconfirmed transactions if needed
    * @param howMany
    */
  def forgetLastBlocks(howMany:Int, removeUnconfirmedTransactions:Boolean):Unit = {
    BlockchainProcessorImpl.getInstance().popOffTo(NxtFunctions.currentHeight - howMany)
    if(removeUnconfirmedTransactions) TransactionProcessorImpl.getInstance().clearUnconfirmedTransactions()
  }
}
