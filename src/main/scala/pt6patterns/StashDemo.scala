package pt6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {
  
  /*
     Resource Actor
     if open => it can receive read/write requests to the resource
     otherwise it will postpone all read/write until the state is open
    
     Resource Actor is closed
     Open => switch to open state
     Read, Write are postponed
    
     Resource Actor is open
     Read, Write are handled
     Close => switch to close state
     
     [Open, Read, Read, Write]
     Switch to open state
     Read
     Read again
     Write
     
     [Read, Open, Write]
     Stash Read
     Switch to open state
     Read
     Write
    */
  case object Open
  case object Close
  case object Read
  case class Write(data: String)
  
  // Mix-in the stash trait
  class ResourceActor extends Actor with ActorLogging with Stash {
    
    private var innerData: String = ""
    
    override def receive: Receive = closed
    
    def closed: Receive = {
      case Open =>
        log.info("Opening the resource")
        unstashAll()
        context.become(open)
      case msg  =>
        log.warning(s"Stashing $msg because I can't handled it while Closed")
        stash()
    }
    def open: Receive = {
      case Read        => log.info(s"I have read: $innerData")
      case Write(data) =>
        log.info("Writing data...")
        innerData = data
      case Close       =>
        log.warning("Closing the resource")
        unstashAll()
        context.become(closed)
      case msg         =>
        log.warning(s"Stashing $msg because I can't handled it while Opened")
        stash()
    }
  }
  
  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor])
  
}
