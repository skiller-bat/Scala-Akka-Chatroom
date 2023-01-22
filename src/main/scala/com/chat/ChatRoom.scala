package com.chat

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.chat.Status.{NOT_OK, OK}

object ChatRoom {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem.create("ChatRoom-System")
    system.actorOf(Props[ChatRoomActor](), "ChatRoom")
  }
}

class ChatRoomActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system
  private var clients: Map[String, ActorRef] = Map()

  override def receive: Receive = {

    case RegisterUser(name) =>
      if (clients contains name) {
        sender ! ResponseRegisterUser(NOT_OK())
      } else {
        clients += (name -> sender())
        sender ! ResponseRegisterUser(OK())
      }

    case msg: Message =>  // dont destruct and construct!
      log.info("Client " + sender + " says: " + msg.text)
      clients.values
        .filter(client => client != sender)
        .foreach(client => client forward Message(msg.text))
  }
}
