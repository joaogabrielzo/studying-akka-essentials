package pt2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {
  
  val system = ActorSystem("actorCapabilitiesDemo")
  
  class SimpleActor extends Actor {
    
    override def receive: Receive = {
      case "Hi!"                          => context.sender() ! "Hello there" // replying to msg
      case msg: String                    => println(s"[$self] I have received the string $msg from ${sender()}")
      case num: Int                       => println(s"[simple actor] I have received the number $num")
      case SpecialMessage(content)        =>
        println(s"[simple actor] I have received the special message $content")
      case SendMsgToSelf(content)         => self ! content
      case SayHiTo(ref)                   => ref ! "Hi!"
      case WirelessPhoneMsg(content, ref) => ref.forward(content + " PINTO")
    }
  }
  
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")
  
  /*
        1 - messages can be of any type if:
        a) messages are IMUTABLE
        b) messages are SERIALIZABLE (JVM can transform it into byte string and send it to another JVM)
            In practice, use case classes and case objects
     */
  
  simpleActor ! "Hello, actor" // string
  simpleActor ! 42 // integer
  
  case class SpecialMessage(content: String)
  
  simpleActor ! SpecialMessage("some special message")
  
  // 2 - Actors have information about their context and about themselves
  // context.self === `this` in POO
  
  case class SendMsgToSelf(content: String)
  
  simpleActor ! SendMsgToSelf("I'm an actor")
  // manda uma msg pro Actor, que manda a msg pra ele mesmo. Como a msg é uma string, o primeiro case a reconhece
  
  // 3 - Actors can reply to msg, using Context
  
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")
  
  case class SayHiTo(ref: ActorRef)
  
  alice ! SayHiTo(bob)
  // o Actor alice vai enviar uma msg para o Actor bob. Por ter como parâmetro "context.sender() ! ",
  // bob vai responder imediatamente, e alice vai receber uma "contra-mensagem"
  
  // 4 - deadLetters
  alice ! "Hi!"
  
  // o Actor alice não pode responder, pq o Sender nesse caso é null.
  // A mensagem vai pro "deadLetters", que é basicamente a lixeira das mensagens.
  
  /*
     5 - Forwarding message
     A -> B -> C
     forwarding = manda a mensagem com o Sender original
     
     As mensagens podem ser modificadas pelo intermediário e ainda manter o Sender. 
     */
  
  case class WirelessPhoneMsg(content: String, ref: ActorRef)
  
  alice ! WirelessPhoneMsg("Hi", bob)
  
  //
  
  
}
