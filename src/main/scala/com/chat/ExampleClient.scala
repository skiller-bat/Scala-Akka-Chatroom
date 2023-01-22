package com.chat

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object Client {

  def main(args: Array[String]): Unit = {

    val myConfig = ConfigFactory.parseString("akka.remote.artery.canonical.port=0")
    val regularConfig = ConfigFactory.load()
    val combined = myConfig.withFallback(regularConfig)
    val complete = ConfigFactory.load(combined)
    val system = ActorSystem("Client-System", complete)

    val client = system.actorOf(Props[ClientActor](), "Client")
    val input = system.actorOf(Props[InputActor](), "Input")
  }
}

class ClientActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system
  private val server = context.actorSelection("akka://Server-System@127.0.0.1:25520/user/Server")
  private var messages: List[Message] = List()

  override def receive: Receive = {

    case Ok() => log.info("OK")
    case NotOk() => log.info("NOT OK")

    case InputMessage(msg) =>
      server ! Message(msg)

    case Message(msg) =>
      log.info(msg)

    case Connect(user) =>
      log.info("got user:" + user)

    case RegisterUser(name) =>
      server ! RegisterUser(name)

    case FindUser(name) =>
      server ! FindUser(name)
  }
}
