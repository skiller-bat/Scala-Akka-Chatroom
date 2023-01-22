package com.chat.client

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.chat.msg.{InputMessage, RegisterUser, ResponseRegisterUser}
import com.typesafe.config.{Config, ConfigFactory}
import com.chat.msg.Status.{NOT_OK, OK}

import scala.concurrent.duration.*
import java.awt.event.{WindowAdapter, WindowEvent}
import java.awt.{BorderLayout, Dimension}
import javax.swing.{JButton, JFrame, JOptionPane, JPanel, JTextArea, JTextField, WindowConstants}
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await

object ClientGUI {

  private val myConfig = ConfigFactory.parseString("akka.remote.artery.canonical.port=0")
  private val regularConfig = ConfigFactory.load()
  private val combined = myConfig.withFallback(regularConfig)
  private val complete = ConfigFactory.load(combined)

  def main(args: Array[String]): Unit = {
    new ClientGUI(complete)
  }
}

class ClientGUI(config: Config) extends JFrame("AkkaChat") {
  implicit val timeout: Timeout = Timeout(1.seconds)
  private val chatPanel = new ChatPanel()
  add(chatPanel)

  pack()

  private val userName = JOptionPane.showInputDialog(this,
    "Enter your unique username",
    "Enter Username",
    JOptionPane.QUESTION_MESSAGE)
  if (userName == null) dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))

  private val system = ActorSystem.create(userName, config)
  private val userActor = system.actorOf(Props[UserActor](), userName)
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  addWindowClosingListener()
  registerUser()

  chatPanel.sendMessage.addActionListener(_ => {
    val input = chatPanel.inputField.getText
    chatPanel.inputField.setText("")
    userActor ! InputMessage(input)
  })

  setVisible(true)

  private def registerUser(): Unit = {
    val response = userActor ? RegisterUser(userName)
    Await.result(response, timeout.duration) match {
      case ResponseRegisterUser(status) =>
        status match {
          case OK =>
          case NOT_OK => dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))
        }
      case _ => dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))
    }
  }

  private def addWindowClosingListener(): Unit = {
    addWindowListener(new WindowAdapter {
      override def windowClosing(e: WindowEvent): Unit =
        super.windowClosing(e)
        system.terminate().onComplete { _ =>
          dispose()
          System.exit(0)
        }
    })
  }

}


class ChatPanel extends JPanel {
  val inputField: JTextField = new JTextField("InputField")
  inputField.setPreferredSize(new Dimension(500, 100))
  val sendMessage: JButton = new JButton("Send")

  val inputPanel = new JPanel(new BorderLayout())
  inputPanel.add(inputField, BorderLayout.CENTER)
  inputPanel.add(sendMessage, BorderLayout.EAST)

  setLayout(new BorderLayout())
  add(inputPanel, BorderLayout.SOUTH)
}
