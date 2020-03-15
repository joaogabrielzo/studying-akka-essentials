package pt2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import ChangingActorBehavior.Mom.{Food, MomStart}

object ChangingActorBehavior extends App {
  
  val system = ActorSystem("changingActorBehavior")
  
  class FussyKid extends Actor {
    
    import FussyKid._
    import Mom._
    
    var state = HAPPY
    override def receive: Receive = {
      case Food(VEGETABLES) => state = SAD
      case Food(CHOCOLATE)  => state = HAPPY
      case Ask(_)           =>
        if (state == HAPPY) sender() ! KidAccepts
        else sender() ! KidRejects
    }
  }
  
  class StatelessFussyKid extends Actor { // boas práticas em vez de var
    
    import FussyKid._
    import Mom._
    
    override def receive: Receive = happyReceive // O "estado inicial" é HAPPY
    
    def happyReceive: Receive = {
      case Food(VEGETABLES) => context.become(sadReceive, false)// change receive handler to sadReceive
      case Food(CHOCOLATE)  =>                                    // true = descarta o handler antigo
      case Ask(_)           => sender() ! KidAccepts              // false = empilha o novo numa pilha de handlers
    }
    
    def sadReceive: Receive = {
      case Food(CHOCOLATE)  => context.unbecome()// change receive handler to happyReceive
      case Food(VEGETABLES) => context.become(sadReceive, false)
      case Ask(_)           => sender() ! KidRejects
    }
  }
  /*
        context.become
        valorInicial - happyReceive
        Food(veg) - happyReceive -> sadReceive
        Food(veg) - happy Receive -> sadReceive -> sadReceive
        Food(choc) - happy Receive -> sadReceive
        
        stack:
        sadReceive - ganhou outro vegetal
        // Quando ele ganha 1 chocolate, o primeiro sadReceive some (unbecome)
        // Como o topo da pilha ainda é sadReceive, ele vai continuar triste.
        sadReceive - ganhou 1 vegetal
        // Se ganhar mais um chocolate, agora sim vai ficar feliz
        happyReceive - valor inicial
     */
  
  object FussyKid {
    case object KidAccepts
    case object KidRejects
    val HAPPY = "happy"
    val SAD = "sad"
  }
  
  class Mom extends Actor {
    
    val log = Logging(system, this)
    
    import FussyKid._
    import Mom._
    
    override def receive: Receive = {
      case MomStart(kidRef) =>
        kidRef ! Food(VEGETABLES)
        kidRef ! Food(VEGETABLES)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Ask("Wanna play?")
      case KidAccepts       => log.info("My kid is happy!")
      case KidRejects       => log.warning("My kid is sad ☹️")
    }
  }
  object Mom {
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(msg: String)
    val VEGETABLES = "veggies"
    val CHOCOLATE = "chocolate"
  }
  
  val fussyKid = system.actorOf(Props[FussyKid], "fussy-kid")
  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid], "stateless-fussy-kid")
  val mom = system.actorOf(Props[Mom], "mom")
  
  mom ! MomStart(statelessFussyKid)
}
