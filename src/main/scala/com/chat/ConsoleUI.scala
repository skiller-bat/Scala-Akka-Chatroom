package com.chat

import akka.actor.{Actor, ActorLogging, ActorSystem}
import com.chat.Status.{NOT_OK, OK}

import scala.io.StdIn.readLine

class ConsoleActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system
  val client = context.actorSelection("/user/User")


  private var clientName = readLine("Username: ")
  client ! RegisterUser(clientName)

  // TODO: use Futures instead of this Actor

  def awaitRegistrationConformation: Receive = {
    case msg: ResponseRegisterUser =>
      msg.status match {
        case OK() =>
          context.become(registered)
          val message = readLine("Message: ")
          client ! InputMessage(message) // TODO
        case NOT_OK() =>
          clientName = readLine("Username already assigned! Enter a different one: ")
          client ! RegisterUser(clientName)
      }

    case _ =>
      log.warning("NOOO")
  }

  def registered: Receive = {
    case msg =>
      log.info(msg.toString)
  }

  override def receive: Receive = awaitRegistrationConformation
}
