package com.chat

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

import scala.io.StdIn.readLine

class InputActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system

  val client = context.actorSelection("/user/Client")


  val ownName = readLine("Username: ")
  client ! RegisterUser(ownName)

//  val otherName = readLine("Contact: ")
//  client ! FindUser(otherName)

  val message = readLine("Message: ")
  client ! InputMessage(message)

  override def receive: Receive = {
    case Ok() => log.info("OK")
  }
}
