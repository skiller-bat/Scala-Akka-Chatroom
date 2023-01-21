package com.remote

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

object Client {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem.create("Client-System")
    system.actorOf(Props[ClientActor](), "Client")
  }
}

class ClientActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system
  private val server = context.actorSelection("akka://Server-System@127.0.0.1:25520/user/Server")
  server ! Message("Hey there")

  override def receive: Receive = {
    case Message(msg) => log.info(msg)
  }
}
