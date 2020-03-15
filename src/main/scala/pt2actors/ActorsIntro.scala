package pt2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsIntro extends App {
  
  // part1 - actor systems
  val actorSystems = ActorSystem("firstActorSystem")
  println(actorSystems.name)
  
  // part2 - create actors
  
  class WordCountActor extends Actor {
    
    // internal data
    var totalWords = 0
    // behavior
    def receive: PartialFunction[Any, Unit] = {
      case message: String =>
        println(s"[word counter] message received: $message")
        totalWords += message.split(" ").length
        println(s"[word counter] message has $totalWords words")
      case msg             => println(s"[word counter] I can not understand ${msg.toString}")
    }
  }
  
  // part3 - instantiate actor
  val wordCounter: ActorRef = actorSystems.actorOf(Props[WordCountActor], "WordCounter")
  val anotherWordCounter: ActorRef = actorSystems.actorOf(Props[WordCountActor], "AnotherWordCounter")
  
  // part4 - communicate with actor
  wordCounter ! "I am learning akka to use with big data" // assíncrono
  anotherWordCounter ! "Another message"
  
  class Person(name: String) extends Actor {
    
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
      case _    => println(s"[$name] Sorry, I didn't understand that.")
    }
  }
  
  val person = actorSystems.actorOf(Props(new Person("Zó"))) // NÃO É BOAS PRÁTICAS
  // Uma classe que extende Actor não pode ser instanciada com "new Class" normalmente, mas dentro do Props é possível
  // para que possa passar seus parâmetros, como na classe Person
  person ! "hi"
  
  object Person {
    
    def props(name: String): Props = Props(new Person(name))
  } // Companion object
  
  val newPerson = actorSystems.actorOf(Person.props("Zó"))
  newPerson ! "test"
  
}
