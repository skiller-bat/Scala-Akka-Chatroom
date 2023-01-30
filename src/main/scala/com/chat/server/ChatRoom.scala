package com.chat.server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import akka.util.Timeout
import com.chat.msg.{Message, MessageACK, MessageRead, RegisterUser, ResponseMessage, ResponseRegisterUser}
import com.chat.msg.Status.{NOT_OK, OK}

import scala.concurrent.duration.*
import scala.::
import scala.collection.mutable

object ChatRoom {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem.create("ChatRoom-System")
    system.actorOf(Props[ChatRoomActor](), "ChatRoom")
  }
}

class ChatRoomActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system
  private var onlineUsers: mutable.Set[ActorRef] = mutable.HashSet()
  private var outgoingMessages: mutable.Map[Message, ActorRef] = mutable.HashMap()

  override def receive: Receive = {

    case RegisterUser(name) =>
      if (!onlineUsers.exists(user => user.path.name == name)) {
        //val userRef = sender()
        onlineUsers = onlineUsers += sender() //sender() :: onlineUsers
        sender() ! ResponseRegisterUser(OK)
      } else {
        sender() ! ResponseRegisterUser(NOT_OK)
      }

    case msg: Message => // dont destruct and construct!
      val senderRef = sender()  //sender() is set to ActorRef.noSender in child actor otherwise
      senderRef ! ResponseMessage(msg)
      log.info("Client " + sender() + " says: " + msg.text)
      val otherUsers = onlineUsers.filter(client => client != senderRef)
      val msgTracker = context.actorOf(Props(new MessageTrackerActor(msg, senderRef, otherUsers)))
      outgoingMessages += (msg -> msgTracker)

    case readMessage: MessageRead =>
      log.info(s"Message ${readMessage.message} read by all users")
      readMessage.originalSender ! readMessage
      outgoingMessages.get(readMessage.message).foreach(tracker => tracker ! PoisonPill.getInstance)
      outgoingMessages -= readMessage.message
  }
}

class MessageTrackerActor(message: Message, originalSender: ActorRef, var otherUsers: mutable.Set[ActorRef]) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    super.preStart()
    log.info(s"Started Message Tracker actor for msg $message from $originalSender")
    if (otherUsers.isEmpty) context.parent ! MessageRead(originalSender, message)
    otherUsers.foreach(client => client ! message)
  }

  override def receive: Receive = {
    case MessageACK(msg) =>
      if (message == msg) otherUsers -= sender();
      log.info(s"Received ACK from ${sender()}")
      if (otherUsers.isEmpty) context.parent ! MessageRead(originalSender, message)
  }
}
