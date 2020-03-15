package pt3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random


class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {
  
  //    setup
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  
  import BasicSpec._
  
  "A Simple Actor" should {
    "send back the same message" in {
      val echoActor = system.actorOf(Props[BasicActor])
      val msg = "hello"
      echoActor ! msg
      
      expectMsg(msg) // Default timeout - 3 segundos
    }
  }
  
  "A Blackhole" should {
    "return no message" in {
      val blackhole = system.actorOf(Props[Blackhole])
      val msg = "hello"
      blackhole ! msg
      
      expectNoMessage(2 second)
    }
  }
  
  "A Lab Test Actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])
    "send back the message in upper case" in {
      val msg = "hello"
      labTestActor ! msg
      
      val reply = expectMsgType[String]
      
      assert(reply == msg.toUpperCase())
    }
    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("hi", "hello")
    }
    "reply with favorite tech" in {
      labTestActor ! "favoriteTech"
      expectMsgAllOf("Scala", "Akka")
    }
  }
}

// companion object pra guardar as informações do teste
object BasicSpec {
  class BasicActor extends Actor {
    
    override def receive: Receive = {
      case message => sender() ! message
    }
  }
  
  class Blackhole extends Actor {
    
    override def receive: Receive = Actor.emptyBehavior
  }
  
  class LabTestActor extends Actor {
    
    val random = new Random()
    override def receive: Receive = {
      case "greeting"      =>
        if (random.nextBoolean()) sender() ! "hi" else sender() ! "hello"
      case "favoriteTech"  =>
        sender() ! "Scala"
        sender() ! "Akka"
      case message: String => sender() ! message.toUpperCase()
    }
  }
}