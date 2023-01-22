package com.chat

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

object Server {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem.create("Server-System") // ?
    system.actorOf(Props[ServerActor](), "Server")
  }
}

// Vermittler
// Telefonbuch
// Chatroom
class ServerActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system
  private var clients: Map[String, ActorRef] = Map()

  override def receive: Receive = {

    case RegisterUser(name) =>
      if (!(clients contains name)) {
        clients += (name -> sender())
        sender() ! Ok()
      } else {
        sender() ! NotOk()
      }

    case FindUser(name) =>
      if (clients contains name)
        sender() ! Connect(clients(name))
      else
        sender() ! NotOk()

    case Message(msg) =>  // dont destruct and construct!
      log.info("Client " + sender() + " says: " + msg)
      clients.values
        .filter(client => client != sender())
        .foreach(client => client forward Message(msg))
  }
}
