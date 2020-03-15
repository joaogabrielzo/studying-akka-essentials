package pt5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Mailboxes extends App {
  
  val system = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))
  
  class SimpleActor extends Actor with ActorLogging {
    
    override def receive: Receive = {
      case msg => log.info(msg.toString)
    }
  }
  
  /**
    * Custom priority mailbox
    * P0 -> most important
    * P1
    * P2
    */
  
  // step 1 - mailbox definition
  class SupportTicketPriority(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(
      PriorityGenerator {
        case message: String if message.startsWith("[P0]") => 0
        case message: String if message.startsWith("[P1]") => 1
        case message: String if message.startsWith("[P2]") => 2
        case message: String if message.startsWith("[P3]") => 3
        case _                                             => 4
      })
  
  
  // step 2 - make it known in configs
  // step 3 - attach the dispatcher to an actor
  
  val supportTicketActor = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
//  supportTicketActor ! "[P3] This thing would be nice to have"
//  supportTicketActor ! "[P0] This thing should be solved NOW"
//  supportTicketActor ! "[P1] dos this when you  want"
  
  /**
    * control-aware mailbox
    * UnboundedControlAwareMailbox
    */
  // step 1 - mark msg as priority
  case object ManagementTicket extends ControlMessage
  
  /*
    step 2 - configure who gets the msg
    - make the actor attach to the mailbox
   */
  // method #1
  val controlAwareMailbox = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))
  controlAwareMailbox ! "[P3] This thing would be nice to have"
  controlAwareMailbox ! "[P0] This thing should be solved NOW"
  controlAwareMailbox ! ManagementTicket
  
  // method #2 - using deployment config
  val altControlAwareActor = system.actorOf(Props[SimpleActor], "altControlAware")
  altControlAwareActor ! "[P3] This thing would be nice to have"
  altControlAwareActor ! "[P0] This thing should be solved NOW"
  altControlAwareActor ! ManagementTicket
}
