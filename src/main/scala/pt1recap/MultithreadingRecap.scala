package pt1recap

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object MultithreadingRecap extends App {
  
  val testThread = new Thread(() => {
    (1 to 1000).foreach(println)
  })
  testThread.start()
  testThread.join()
  
  class BankAccount(@volatile private var amount: Int) { // @volatile torna o paramêtro atomico
    override def toString: String = "" + amount
    
    def withdraw(money: Int): Unit = this.amount -= money
    // Não é seguro pq duas threads podem ler/escrever ao mesmo tempo.
    
    def safeWithdraw(money: Int) = this.synchronized {
      this.amount -= money
    } // Numa expressão sincronizada, duas threads não podem
    // acessar ao mesmo tempo, então é seguro (atomica)
  }
  
  // Scala Futures
  val future = Future {
    // long computation - on a different thread
    42
  }
  future.onComplete {
    case Success(x) => println("Got a " + 42)
    case Failure(_) => throw new RuntimeException("DEU ERRADO")
  }
  
  val aProcessedFuture = future.map(_ + 2)
  val aFlatFuture = future.flatMap(value => Future(value + 2))
  val aFilteredFuture = future.filter(_ % 2 == 0)
  
  val aNonSenseFuture = for {
    meaningOfLife <- future
    filteredMeaning <- aFilteredFuture
  } yield (meaningOfLife + filteredMeaning)
  
}
