package com.chat

import akka.actor.{ActorSystem, Props}
import com.chat.client.ClientGUI
import com.chat.server.ChatRoomActor
import com.typesafe.config.ConfigFactory

object ChatDemo {
  private val myConfig = ConfigFactory.parseString("akka.remote.artery.canonical.port=0")
  private val regularConfig = ConfigFactory.load()
  private val combinedConfig = myConfig.withFallback(regularConfig)
  private val completeConfig = ConfigFactory.load(combinedConfig)

  def main(args: Array[String]): Unit = {
    val system = ActorSystem.create("ChatRoom-System")
    system.actorOf(Props[ChatRoomActor](), "ChatRoom")

    new ClientGUI(completeConfig, "Bob")
    new ClientGUI(completeConfig, "Alice")
    new ClientGUI(completeConfig, "Chris")
    new ClientGUI(completeConfig, "Jennifer")
  }
}
