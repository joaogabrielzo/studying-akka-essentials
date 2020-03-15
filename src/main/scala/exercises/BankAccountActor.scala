package exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import exercises.BankAccountActor.BankAccount.{Deposit, Statement, Transfer, Withdraw}

object BankAccountActor extends App {
  
  val system = ActorSystem("bank-account")
  
  class BankAccount extends Actor {
    
    val log = Logging(system, this)
    
    import BankAccount._
    
    var funds: Double = 0
    
    override def receive: Receive = {
      case Statement             => log.info(s"Your balance is $funds")
      case Deposit(amount)       =>
        if (amount <= 0) log.error("Value less or equal than 0")
        else {
          funds += amount
          log.info(s"Deposit succeeded. Your balance is $funds")
        }
      case Withdraw(amount)      =>
        if (amount <= 0) log.error("Value less or equal than 0")
        else {
          funds -= amount
          log.info(s"Withdraw succeeded. Your balance is $funds")
        }
      case Transfer(amount, ref) =>
        if (amount <= 0) log.error("Value less or equal than 0")
        else {
          funds -= amount
          ref ! Deposit(amount)
          log.info(s"You have successfuly transferred $amount to $ref. " +
                   s"Your balance is $funds")
        }
    }
  }
  
  object BankAccount {
    
    case class Deposit(amount: Double)
    case class Withdraw(amount: Double)
    case class Transfer(amount: Double, ref: ActorRef)
    case object Statement
  }
  
  val bankAccount: ActorRef = system.actorOf(Props[BankAccount], "bankAccount")
  val anotherAccount: ActorRef = system.actorOf(Props[BankAccount], "anotherAccount")
  
  bankAccount ! Deposit(10000)
  bankAccount ! Transfer(4000, anotherAccount)
  bankAccount ! Statement
  Thread.sleep(500)
  anotherAccount ! Withdraw(2000)
  anotherAccount ! Statement
}