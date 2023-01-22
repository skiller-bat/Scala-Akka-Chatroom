package com.chat

case class RegisterUser(name: String)
case class ResponseRegisterUser(status: Status)

case class Message(text: String)
case class ResponseMessage(msg: Message)
case class InputMessage(text: String)

case class Retry(msg: Message)

enum Status:
  case OK, NOT_OK
