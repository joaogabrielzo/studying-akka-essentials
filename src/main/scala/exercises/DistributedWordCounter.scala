package exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object DistributedWordCounter extends App {
  
  val system = ActorSystem("distributed-word-counter")
  
  object WordCounterMaster {
    
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: List[(String, Int)])
  }
  class WordCounterMaster extends Actor {
    
    import WordCounterMaster._
    
    override def receive: Receive = {
      case Initialize(n) =>
        println("Initializing...")
        val childrenRefs =
          for (i <- 1 to n) yield context.actorOf(Props[WordCounterWorker], s"worker-$i")
        context.become(withChildren(childrenRefs, 0, 0, Map()))
    }
    
    def withChildren(childrenRefs: Seq[ActorRef], currentChildIndex: Int, currentTaskId: Int,
                     requestMap: Map[Int, ActorRef]): Receive = {
      case text: String              =>
        println(s"Sending task #$currentTaskId to worker-$currentChildIndex")
        val originalSender = sender()
        val task = WordCountTask(currentTaskId, text)
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        val next = (currentChildIndex + 1) % childrenRefs.length
        val nextTaskId = currentTaskId + 1
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(childrenRefs, next, nextTaskId, newRequestMap))
      case WordCountReply(id, count) =>
        println(s"The count for task #$id is $count")
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(withChildren(childrenRefs, currentChildIndex, currentTaskId, requestMap - 1))
    }
  }
  
  class WordCounterWorker extends Actor {
    
    import WordCounterMaster._
    
    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"Received task #$id")
        val actualText = text.replace(",", "")
                             .replace(".", "")
        val textToLower = actualText.toLowerCase
        val splitText: Array[String] = textToLower.split(" ")
        val mapText = splitText.map(x => (x, 1))
        val mapReduceText: List[(String, Int)] = mapText.groupBy(_._1).mapValues(_.map(_._2).sum).toList
        sender() ! WordCountReply(id, mapReduceText)
    }
  }
  
  object TaskManager {
    case class requestTask[A](task: A, nWorkers: Int)
  }
  class TaskManager extends Actor {
    
    import TaskManager._
    import WordCounterMaster._
    
    override def receive: Receive = {
      case requestTask(task, nWorkers) =>
        val master = context.actorOf(Props[WordCounterMaster], "master")
        master ! Initialize(nWorkers)
        task match {
          case x: String       => master ! x
          case x: List[String] => x.foreach(text => master ! text)
        }
      case count: Int                  => println(s"[task reply] The count of words is $count")
    }
  }
  
  val taskManager = system.actorOf(Props[TaskManager], "manager")
  
  taskManager ! TaskManager.requestTask(List("distributed word counter", "two words words", "Ok oK ok OK"), 4)
  
}
