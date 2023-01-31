package com.chat.client

import akka.actor.Status.{Failure, Success}
import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.chat.msg.Status.{NOT_OK, OK}
import com.chat.*
import com.chat.msg.{InputMessage, Message, MessageACK, MessageRead, RegisterUser, ResponseMessage, ResponseRegisterUser, Retry}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.io.StdIn.readLine
import scala.util.control.Breaks.{break, breakable}
import scala.collection.mutable.ListBuffer

object User {

  implicit val timeout: Timeout = Timeout(1.seconds)

  def main(args: Array[String]): Unit = {

    val myConfig = ConfigFactory.parseString("akka.remote.artery.canonical.port=0")
    val regularConfig = ConfigFactory.load()
    val combined = myConfig.withFallback(regularConfig)
    val complete = ConfigFactory.load(combined)
    val system = ActorSystem("User-System", complete)

    val client = system.actorOf(Props[UserActor](), "User")
    //    val input = system.actorOf(Props[ConsoleActor](), "Input")

    /* Register User */

    var username = readLine("Username: ")
    breakable {
      while (true) {
        val res = client ? RegisterUser()
        Await.result(res, 1.second) match {
          case ResponseRegisterUser(status) =>
            status match {
              case OK => break
              case NOT_OK => username = readLine("Username already assigned! Enter a different one: ")
            }
        }
      }
    }

    /* Communicate */
    var message = readLine("Message: ")
    while (message != "") {
      client ! InputMessage("", message)
      message = readLine("Message: ")
    }

    println("Goodbye " + username + "!")
    // TODO: shutdown
  }
}

class UserActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system
  implicit val dispatcher: ExecutionContextExecutor = context.dispatcher
  private val server = context.actorSelection("akka://ChatRoom-System@127.0.0.1:25520/user/ChatRoom")
  private var ui = Actor.noSender
  private val outgoing = mutable.Map[Message, Option[Cancellable]]()
  private val unreadMessages = ListBuffer[Message]()

  override def receive: Receive = expectRegistration

  def expectRegistration: Receive = {
    case msg: RegisterUser =>
      server ! msg
      ui = sender()
      context.become(awaitRegistrationConformation)
  }

  def awaitRegistrationConformation: Receive = {
    case msg: ResponseRegisterUser =>
      ui ! msg
      msg.status match {
        case OK =>
          context.become(registered)
        case NOT_OK =>
          context.become(expectRegistration)
      }
  }

  def registered: Receive = {
    case inputMessage: InputMessage =>
      val message = Message(inputMessage.userName, inputMessage.text)
      server ! message
      unreadMessages += message
      val cancellable = system.scheduler.scheduleOnce(3.seconds, self, Retry(message))
      outgoing += (message -> Option(cancellable))
      ui ! inputMessage

    case response: ResponseMessage =>
      outgoing(response.msg) match
        case Some(cancellable) =>
          cancellable.cancel()
          outgoing(response.msg) = None
          ui ! response
        case None =>

    case Retry(message) =>
      server ! message
      outgoing(message) = None

    case message: Message =>
      log.info(message.text)
      sender() ! MessageACK(message)
      ui ! message

    case MessageRead(_, message) =>
      log.info(s"Message $message has been read by everyone")
      unreadMessages -= message
      ui ! MessageRead(self, message)
  }
}
