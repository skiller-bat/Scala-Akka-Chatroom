package com.chat

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.chat.Status.{NOT_OK, OK}
import com.typesafe.config.ConfigFactory

object User {

  def main(args: Array[String]): Unit = {

    val myConfig = ConfigFactory.parseString("akka.remote.artery.canonical.port=0")
    val regularConfig = ConfigFactory.load()
    val combined = myConfig.withFallback(regularConfig)
    val complete = ConfigFactory.load(combined)
    val system = ActorSystem("User-System", complete)

    val client = system.actorOf(Props[UserActor](), "User")
    val input = system.actorOf(Props[ConsoleActor](), "Input")
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
