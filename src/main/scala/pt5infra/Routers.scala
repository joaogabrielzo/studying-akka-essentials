package pt5infra

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

object Routers extends App {
  class Master extends Actor {
    
    //    step 1 - create routees
    // created 5 based off Worker actor
    private val workers = for (i <- 1 to 5) yield {
      val worker = context.actorOf(Props[Worker], s"worker-$i")
      context.watch(worker)
      ActorRefRoutee(worker)
    }
    // step 2 - define router
    private var router = Router(RoundRobinRoutingLogic(), workers)
    
    override def receive: Receive = {
      // step 4 - handle the termination of the routees
      case Terminated(ref) =>
        router = router.removeRoutee(ref)
      // step 3 -route the messages
      case msg =>
        router.route(msg, sender())
    }
  }
  
  class Worker extends Actor with ActorLogging {
    
    override def receive: Receive = {
      case msg => log.info(msg.toString)
    }
  }
  
  val system = ActorSystem("RoutersDemo", ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master])

//  for (i <- 1 to 10) {
//    master ! s"[$i] Hello from the world"
//  }
  
  /**
    * Method 2
    * Master with it's own children
    */
  // 2.1 - from code
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Worker]), "simple-pool-master")
  for (i <- 1 to 12) {
    poolMaster ! s"[$i] Hello from the world"
  }
  
  // 2.2 - from config
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Worker]), "poolMaster2")
//  for (i <- 1 to 7) {
//    poolMaster2 ! s"[$i] Hello world"
//  }
  
  /**
    * Method 3
    * Routers with actors created elsewhere
    */
  val workerList: List[ActorRef] = (1 to 5).map(i => system.actorOf(Props[Worker], s"worker_$i")).toList
  
  val workersPath = workerList.map(workerRef => workerRef.path.toString)
  
  val groupMaster = system.actorOf(RoundRobinGroup(workersPath).props())
  for (i <- 1 to 10) {
    groupMaster ! s"[$i] Hello world"
  }
  
}
