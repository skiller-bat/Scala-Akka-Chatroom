package com.chat.client

import akka.actor.{AbstractActor, Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.chat.msg.{InputMessage, Message, MessageRead, RegisterUser, ResponseMessage, ResponseRegisterUser}
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
  private val combinedConfig = myConfig.withFallback(regularConfig)
  private val completeConfig = ConfigFactory.load(combinedConfig)

  def main(args: Array[String]): Unit = {
    new ClientGUI(completeConfig, "Bob")
    new ClientGUI(completeConfig, "Alice")
    new ClientGUI(completeConfig, "Chris")
    new ClientGUI(completeConfig, "Jennifer")
  }
}

class ClientGUI(config: Config, var userName: String = "") extends JFrame("AkkaChat") {
  implicit val timeout: Timeout = Timeout(3.seconds)
  private val chatPanel = new ChatPanel()
  add(chatPanel)

  pack()
  if (userName == "") {
    userName = JOptionPane.showInputDialog(this, "Enter your unique username",
      "Enter Username",
      JOptionPane.QUESTION_MESSAGE)
  }

  if (userName == null || userName.isEmpty) dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))

  private val system = ActorSystem.create(s"${userName}System", config)
  private val userActor = system.actorOf(Props[UserActor](), userName)
  private val listenerActor = system.actorOf(Props(new Actor with ActorLogging {
    private var ui = ActorRef.noSender

    override def receive: Receive = {
      case msg: RegisterUser => ui = sender(); userActor ! RegisterUser(userName)
      case msg: ResponseRegisterUser => setTitle(s"AkkaChat - $userName | Online"); ui ! msg
      case msg: InputMessage => chatPanel.display.append(s"${msg.userName}: ${msg.text}\n");
      case msg: ResponseMessage => chatPanel.display.append(s"Server received message ${msg.msg}\n")
      case msg: MessageRead => chatPanel.display.append(s"${msg.message} has been read by everyone\n")
      case msg: Message => chatPanel.display.append(s"${msg.userName}: ${msg.text}\n")
    }
  }), s"${userName}Listener")

  setTitle(s"AkkaChat - $userName | Offline")
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  addWindowClosingListener()
  registerUser()

  chatPanel.sendMessage.addActionListener(_ => {
    val input = chatPanel.inputField.getText
    chatPanel.inputField.setText("")
    userActor.tell(InputMessage(userName, input), listenerActor) //We use a different sender here
  })

  setVisible(true)

  private def registerUser(): Unit = {
    val response = listenerActor ? RegisterUser(userName)
    Await.result(response, timeout.duration) match {
      case ResponseRegisterUser(status) =>
        status match {
          case OK =>
          case NOT_OK => dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))
        }
      case _ => println("Ooops!") // dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))
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
  val display: JTextArea = new JTextArea("")
  display.setPreferredSize(new Dimension(500, 700))
  val inputField: JTextField = new JTextField("")
  inputField.setPreferredSize(new Dimension(500, 100))
  val sendMessage: JButton = new JButton("Send")

  val inputPanel = new JPanel(new BorderLayout())
  inputPanel.add(inputField, BorderLayout.CENTER)
  inputPanel.add(sendMessage, BorderLayout.EAST)

  setLayout(new BorderLayout())
  add(display, BorderLayout.CENTER)
  add(inputPanel, BorderLayout.SOUTH)
}
