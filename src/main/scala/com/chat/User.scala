package com.chat

import akka.actor.Status.{Failure, Success}
import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props}
import akka.pattern.ask
import akka.util.Timeout
import Status.{NOT_OK, OK}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
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
    implicit val timeout: Timeout = Timeout(1.seconds)
    var username = readLine("Username: ")
    breakable { while (true) {
      val res = client ? RegisterUser(username)
      Await.result(res, 1.second) match {
        case ResponseRegisterUser(status) =>
          status match {
            case OK => break
            case NOT_OK => username = readLine("Username already assigned! Enter a different one: ")
          }
      }
    }}

    /* Communicate */
    var message = readLine("Message: ")
    while (message != "") {
      client ! InputMessage(message)
      message = readLine("Message: ")
    }

    println("Goodbye " + username + "!")
    // TODO: shutdown
  }
}

class UserActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system
  private val server = context.actorSelection("akka://ChatRoom-System@127.0.0.1:25520/user/ChatRoom")
  private var input = Actor.noSender
  private var outgoing = scala.collection.mutable.Map[Message, Option[Cancellable]]()


  def expectRegistration: Receive = {
    case msg: RegisterUser =>
      server ! msg
      input = sender()
      context.become(awaitRegistrationConformation)
  }

  def awaitRegistrationConformation: Receive = {
    case msg: ResponseRegisterUser =>
      input ! msg
      msg.status match {
        case OK =>
          context.become(registered)
        case NOT_OK =>
          context.become(expectRegistration)
      }
  }

  def registered: Receive = {

    case InputMessage(msg) =>
      val m = Message(msg)
      server ! m

      import system.dispatcher
      val c = system.scheduler.scheduleOnce(3.seconds, self, Retry(m))
      outgoing += (m -> Option(c))

    case ResponseMessage(msg) =>
      outgoing(msg).get.cancel()
      outgoing(msg) = None

    case Retry(msg) =>
      server ! msg
      outgoing(msg) = None

    case Message(msg) =>
      log.info(msg)
  }

  override def receive: Receive = expectRegistration
}
