package com.chat

case class RegisterUser(name: String)
case class ResponseRegisterUser(status: Status)

case class Message(text: String)
case class InputMessage(text: String)


enum Status:
  case OK, NOT_OK

/*sealed abstract class Status
object Status {
  final case class OK() extends Status
  final case class NOT_OK() extends Status
}*/
