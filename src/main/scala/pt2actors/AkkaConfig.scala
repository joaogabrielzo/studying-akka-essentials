package pt2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object AkkaConfig extends App {
  
  /**
    * Inline configuration
    */
  
  val configString =
    """
        |akka {
        |   loglevel = "ERROR"
        |}
    """.stripMargin
  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("ConfigurationDemo", ConfigFactory.load(config))
  
  class SimpleLoggingActor extends Actor with ActorLogging {
    
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }
  val simpleLog = system.actorOf(Props[SimpleLoggingActor])
  simpleLog ! "test message"
  
  val defaultConfigFileSystem = ActorSystem("DefaultConfigFile")
  // Quando não é passada uma configuração ao system, ele procura direto em main/resources/application.conf
  val defaultConfigActor = defaultConfigFileSystem.actorOf(Props[SimpleLoggingActor])
  defaultConfigActor ! "testing log"
  
  /**
    * Separate config in the same file
    */
  
  val specialConf = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("SpecialConfigDemo", specialConf)
  val specialActor = specialConfigSystem.actorOf(Props[SimpleLoggingActor])
  specialActor ! "testing special"
  
  /**
    * Separate config in another file
    */
  
  val separateConfig = ConfigFactory.load("secret/secretConfig.conf")
  println(s"separate config file log leve: ${separateConfig.getString("akka.loglevel")}")
  
  /**
    * Different file format
    * JSON
    */
  
  val jsonConfig = ConfigFactory.load("json/jsonConfig.conf")
  println(s"json config: ${jsonConfig.getString("jsonProperty")}")
  println(s"json config: ${jsonConfig.getString("akka.loglevel")}")
  
  // PROPERTIES
  
  val propsConfig = ConfigFactory.load("props/propsConf.properties")
  println(s"Property configs: ${propsConfig.getString("my.simpleProperty")}")
  println(s"Property configs: ${propsConfig.getString("akka.loglevel")}")
}
