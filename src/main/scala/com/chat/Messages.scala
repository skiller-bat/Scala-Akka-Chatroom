package com.chat

import akka.actor.ActorRef

case class RegisterUser(name: String)
case class FindUser(name: String)

case class Connect(user: ActorRef)

case class Message(msg: String)

case class InputMessage(msg: String)

case class Ok()
case class NotOk()
