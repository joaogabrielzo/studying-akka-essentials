package pt2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import ChildActors.Parent.{CreateChild, TellChild}

object ChildActors extends App {
  
  val system = ActorSystem("child-actors")
  
  object Parent {
    case class CreateChild(name: String)
    case class TellChild(msg: String)
  }
  class Parent extends Actor {
    
    import Parent._
    
    val log = Logging(system, this)
    
    override def receive: Receive = {
      case CreateChild(name) =>
        log.info(s"Creating child")
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }
    
    def withChild(childRef: ActorRef): Receive = {
      case TellChild(msg) => childRef forward msg
    }
  }
  
  class Child extends Actor {
    
    val log = Logging(system, this)
    
    override def receive: Receive = {
      case msg => log.info(s"I got: $msg")
    }
  }
  
  val parent = system.actorOf(Props[Parent], "parentActor")
  
  parent ! CreateChild("zo")
  parent ! TellChild("Coé")
  
  /*
        Guardian Actors (top-level) (parent do parent)
        - /system = system guardian
        - /user = user-level guardian
        - / = root guardian
     */
  
  // Actor Selection
  
  val childSelection = system.actorSelection("/user/parentActor/zo")
  childSelection ! "Coé via selection"
  
  /**
    * Nunca passar estados mutáveis ou a referência "THIS" para Child Actors.
    */
}
