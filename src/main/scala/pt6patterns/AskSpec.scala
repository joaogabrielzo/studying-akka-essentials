package pt6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  
  import AskSpec._
  
  "An authenticator" should {
    authenticatorTestSuite(Props[AuthManager])
  }
  "A piped authenticator" should {
    authenticatorTestSuite(Props[PipedAuthManager])
  }
  
  def authenticatorTestSuite(props: Props): Unit = {
    import AuthManager._
    val authManager = system.actorOf(props)
    "fail to authenticate a non-registered user" in {
      authManager ! Authenticate("zo", "realpassword")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }
    "fail to authenticate if wrong password" in {
      authManager ! RegisterUser("zo", "realpassword")
      authManager ! Authenticate("zo", "fakepassword")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }
    "success to authenticate with right password" in {
      authManager ! RegisterUser("zo", "realpassword")
      authManager ! Authenticate("zo", "realpassword")
      expectMsg(AuthSuccess)
    }
  }
}

/**
  * ACTUAL CODE BELOW
  * NEVER CALL METHODS ON THE ACTOR INSTANCE OR ACCESS MUTABLE STATE IN ONCOMPLETE/CALLBACKS.
  */

object AskSpec {
  // this code is somewhere else in application
  
  case class Read(key: String)
  case class Write(key: String, value: String)
  
  class KVActor extends Actor with ActorLogging {
    
    override def receive: Receive = online(Map())
    
    def online(kv: Map[String, String]): Receive = {
      case Read(key)         =>
        log.info(s"Trying to read the value from key $key")
        sender() ! kv.get(key)
      case Write(key, value) =>
        log.info(s"Write the value $value to the key $key")
        context.become(online(kv + (key -> value)))
    }
  }
  
  // user authenticator actor
  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case class AuthFailure(msg: String)
  case object AuthSuccess
  object AuthManager {
    
    val AUTH_FAILURE_NOT_FOUND = "User not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "password incorrect"
    val AUTH_FAILURE_SYSTEM_ERROR = "system error"
  }
  
  class AuthManager extends Actor with ActorLogging {
    
    import AuthManager._
    
    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher
    protected val authDB = context.actorOf(Props[KVActor])
    
    override def receive: Receive = {
      case RegisterUser(username, password) => authDB ! Write(username, password)
      case Authenticate(username, password) => handleAuthentication(username, password)
    }
    
    def handleAuthentication(username: String, password: String) = {
      val originalSender = sender()
      // ask the actor
      val future = authDB ? Read(username)
      // handle the future
      future.onComplete {
        // NEVER CALL METHODS ON THE ACTOR INSTANCE OR ACCESS MUTABLE STATE IN ONCOMPLETE.
        case Success(None)             => originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if (dbPassword == password) originalSender ! AuthSuccess
          else originalSender ! AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case Failure(_)                => originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM_ERROR)
      }
    }
  }
  
  class PipedAuthManager extends AuthManager {
    
    import AuthManager._
    
    override def handleAuthentication(username: String, password: String): Unit = {
      // ask the actor
      val future = authDB ? Read(username) // Future[Any]
      // process the future until get the response
      val passwordFuture = future.mapTo[Option[String]] // Future[Option[String]]
      val responseFuture = passwordFuture.map {
        case None             => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if (dbPassword == password) AuthSuccess
          else AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
      } // Future[Any] - will be completed with the response I will send back
      
      // pipe the resulting future to the actor you want to send the result to
      /*
        When the future completes, send the response to the ActorRef in the args list
       */
      responseFuture.pipeTo(sender())
    }
  }
  
}
