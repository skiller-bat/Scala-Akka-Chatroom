package com.chat

import akka.actor.Status.{Failure, Success}
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.chat.Status.{NOT_OK, OK}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.io.StdIn.readLine
import scala.util.control.Breaks.{break, breakable}

object User {

  def main(args: Array[String]): Unit = {

    val myConfig = ConfigFactory.parseString("akka.remote.artery.canonical.port=0")
    val regularConfig = ConfigFactory.load()
    val combined = myConfig.withFallback(regularConfig)
    val complete = ConfigFactory.load(combined)
    val system = ActorSystem("User-System", complete)

    val client = system.actorOf(Props[UserActor](), "User")
//    val input = system.actorOf(Props[ConsoleActor](), "Input")

    /* Register User */
    implicit val timeout = Timeout(1.second)
    var username = readLine("Username: ")
    breakable { while (true) {
      val res = client ? RegisterUser(username)
      Await.result(res, 1.second) match {
        case ResponseRegisterUser(status) =>
          status match {
            case OK() => break
            case NOT_OK() => username = readLine("Username already assigned! Enter a different one: ")
          }
      }
    }}

    /* Communicate */
    var message = readLine("Message: ")
    do {
      client ! InputMessage(message)
      message = readLine("Message: ")
    } while (message != "")

    println("Goodbye " + username + "!")
    // TODO: shutdown
  }
}

class UserActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system
  private val server = context.actorSelection("akka://ChatRoom-System@127.0.0.1:25520/user/ChatRoom")
  private var input = Actor.noSender
  private var messages: List[Message] = List()


  def expectRegistration: Receive = {
    case msg: RegisterUser =>
      server ! msg
      input = sender
      context.become(awaitRegistrationConformation)

    case _ =>
      log.warning("NOOO")
  }

  def awaitRegistrationConformation: Receive = {
    case msg: ResponseRegisterUser =>
      input ! msg
      msg.status match {
        case OK() =>
          context.become(registered)
        case NOT_OK() =>
          context.become(expectRegistration)
      }

    case _ =>
      log.warning("NOOO")
  }

  def registered: Receive = {

    case InputMessage(msg) =>
      server ! Message(msg)

    case Message(msg) =>
      log.info(msg)
  }

  override def receive: Receive = expectRegistration
}
