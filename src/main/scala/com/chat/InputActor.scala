package com.chat

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.io.StdIn.readLine

/*
  console UI to tell ClientActor to text some other client
 */
/*
  also, how are messages (all of them) acknowledged?
  (synchronization between actors?!)
 */

object Input {

  def main(args: Array[String]): Unit = {

    val myConfig = ConfigFactory.parseString("akka.remote.artery.canonical.port=0")
    val regularConfig = ConfigFactory.load()
    val combined = myConfig.withFallback(regularConfig)
    val complete = ConfigFactory.load(combined)

//    val system = ActorSystem.create("Input-System", complete) // ?
    val system = ActorSystem.create("Client-System", complete) // ?
    system.actorOf(Props[InputActor](), "Input")
  }
}


class InputActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system

  val client = context.actorSelection("/user/Client")
//  val client = context.actorSelection("/Client-System/user/Client")
//  val client = context.actorSelection("akka://Client-System@127.0.0.1:23756/user/Client")
//  val client = context.actorSelection("akka://Client-System/user/Client")
//  println(client)

  val ownName = readLine("Username: ")
  client ! RegisterUser(ownName)

  val otherName = readLine("Contact: ")
  client ! FindUser(otherName)

  override def receive: Receive = {
    case Ok() => log.info("OK")
  }
}
