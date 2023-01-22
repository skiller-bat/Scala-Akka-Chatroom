package com.chat.server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import com.chat.msg.{Message, MessageACK, MessageRead, RegisterUser, ResponseMessage, ResponseRegisterUser}
import com.chat.msg.Status.{NOT_OK, OK}

import scala.::

object ChatRoom {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem.create("ChatRoom-System")
    system.actorOf(Props[ChatRoomActor](), "ChatRoom")
  }
}

class ChatRoomActor extends Actor with ActorLogging {

  implicit val system: ActorSystem = context.system
  private var onlineUsers: List[ActorRef] = List()
  private var outgoingMessages: Map[Message, ActorRef] = Map()

  override def receive: Receive = {

    case RegisterUser(name) =>
      if (!onlineUsers.exists(user => user.path.name == name)) {
        onlineUsers = sender() :: onlineUsers
        sender() ! ResponseRegisterUser(OK)
      } else {
        sender() ! ResponseRegisterUser(NOT_OK)
      }

    case msg: Message => // dont destruct and construct!
      log.info("Client " + sender() + " says: " + msg.text)
      val otherUsers = onlineUsers.filter(client => client != sender())
      val msgTracker = context.actorOf(Props(new MessageTrackerActor(msg, sender(), otherUsers)))
      outgoingMessages += (msg -> msgTracker)
      sender() ! ResponseMessage(msg)

    case readMessage: MessageRead =>
      log.info(s"Message ${readMessage.message} read by all users")
      readMessage.originalSender ! readMessage
      outgoingMessages.get(readMessage.message).foreach(tracker => tracker ! PoisonPill.getInstance)
      outgoingMessages -= readMessage.message
  }
}

class MessageTrackerActor(message: Message, originalSender: ActorRef, var otherUsers: List[ActorRef]) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    super.preStart()
    log.info(s"Started Message Tracker actor for msg $message")
    otherUsers.foreach(client => client ! message)
  }

  override def receive: Receive = {
    case MessageACK(msg) =>
      if (message == msg) otherUsers = otherUsers.filter(_ != sender()); log.info(s"Received ACK from ${sender()}")
      if (otherUsers.isEmpty) context.parent ! MessageRead(originalSender, message)
  }
}
