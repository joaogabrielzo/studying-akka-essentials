package pt5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._

object Scheduling extends App {
  
  val system = ActorSystem("scheduling")
  
  import system.dispatcher
  
  class SimpleActor extends Actor with ActorLogging {
    
    override def receive: Receive = {
      case msg => log.info(msg.toString)
    }
  }
  val simpleActor = system.actorOf(Props[SimpleActor], "simple-actor")

//  system.log.info("scheduling reminder for simple actor")
//
//  system.scheduler.scheduleOnce(2 second) {
//    simpleActor ! "reminder"
//  }//(system.dispatcher) agora já existe um valor implícito, não precisa mais declarar
//  system.log.info("this runs after")
//
//  val routine: Cancellable = system.scheduler.schedule(1 second, 2 seconds) {
//    simpleActor ! "heartbeat"
//  } // a cada 2 segundos vai checar se o ator responder
//
//  system.scheduler.scheduleOnce(5 seconds) {
//    routine.cancel()
//  }// como é um Cancellable, pode ser cancelado a qualquer momento
  
  class SelfClosingActor extends Actor with ActorLogging {
    
    var schedule = createTimeOutWindow()
    def createTimeOutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second) {
        self ! "timeout"
      }
    }
    override def receive: Receive = {
      case "timeout" =>
        log.warning("stopping instance")
        context.stop(self)
      case msg       =>
        log.info(s"Received $msg, going on")
        schedule.cancel()
        schedule = createTimeOutWindow()
    }
  }

//  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "self-closing")
//  system.scheduler.scheduleOnce(250 millis) {
//    selfClosingActor ! "ping"
//  }
//  system.scheduler.scheduleOnce(2 seconds) {
//    system.log.info("sending pong to self closing actor")
//    selfClosingActor ! "pong"
//  }
  
  /**
    * Timer
    */
  
  case object TimerKey
  case object Start
  case object Reminder
  case object Stop
  class TimerBasedHeartbeatActor extends Actor with ActorLogging with Timers {
    
    timers.startSingleTimer(TimerKey, Start, 500 millis)
    
    override def receive: Receive = {
      case Start    =>
        log.info("Bootstrapping")
        timers.startPeriodicTimer(TimerKey, Reminder, 1 second)
      case Reminder =>
        log.info("alive")
      case Stop     =>
        log.warning("stopping")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }
  
  val timerActor = system.actorOf(Props[TimerBasedHeartbeatActor], "timer")
  system.scheduler.scheduleOnce(5 seconds) {
    timerActor ! Stop
  }
  
}
