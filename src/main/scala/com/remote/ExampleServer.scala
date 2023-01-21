package com.remote

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

object Server {

  def main(args: Array[String]): Unit = {

//    val system = ActorSystem("Server-System")
    val system = ActorSystem.create("Server-System")
    system.actorOf(Props[ServerActor](), "Server")
  }
}

class ServerActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system

  override def receive: Receive = {
    case Message(msg) => {
      log.info(msg)
      sender() ! Message("got it")
    }
  }
}
