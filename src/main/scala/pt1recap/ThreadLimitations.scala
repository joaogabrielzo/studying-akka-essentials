package pt1recap

object ThreadLimitations extends App {
  
  class BankAccount(private var amount: Int) { // @volatile torna o paramêtro atomico
    override def toString: String = "" + amount
    
    def withdraw(money: Int): Unit = this.amount -= money
    // Não é seguro pq duas threads podem ler/escrever ao mesmo tempo.
    def deposit(money: Int): Unit = this.amount += money
    def getAmount: Int = amount
  }
  
  val account = new BankAccount(2000)
  for (_ <- 1 to 1000) {
    new Thread(() => account.deposit(1)).start()
  }
  for (_ <- 1 to 1000) {
    new Thread(() => account.withdraw(1)).start()
  }
  println(account.getAmount)
  
  var task: Runnable = null
  val runningThread: Thread = new Thread(() => {
    while (true) {
      while (task == null) {
        runningThread.synchronized {
          println("[background] waiting for a task")
          runningThread.wait()
        }
      }
      task.synchronized {
        println("[background] got something")
        task.run()
        task = null
      }
    }
  })
  def delegateToBrackground(r: Runnable) = {
    if (task == null) task = r
    
    runningThread.synchronized {
      runningThread.notify()
    }
  }
  
  runningThread.start()
  Thread.sleep(500)
  delegateToBrackground(() => println(42))
  Thread.sleep(1000)
  delegateToBrackground(() => println("Qualquer coisa on background"))
  runningThread.interrupt()
}
