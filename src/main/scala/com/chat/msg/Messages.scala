package com.chat.msg

import akka.actor.ActorRef

case class RegisterUser(name: String)
case class ResponseRegisterUser(status: Status)

case class Message(text: String)    //Normal chat message
case class ResponseMessage(msg: Message)  //Chatroom confirms message incoming arrival
case class InputMessage(text: String) //Used for distinguishing messages from ui and other messages

case class Retry(msg: Message)  //Retry message if chatroom does not respond

case class MessageACK(message: Message) //ACK message arrival on receiving end

case class MessageRead(originalSender: ActorRef, message: Message)  //Confirm all recipients have sent ack in response to msg

enum Status:
  case OK, NOT_OK
