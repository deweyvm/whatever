package com.deweyvm.dogue.net

import com.deweyvm.dogue.common.threading.Task
import com.deweyvm.dogue.Game
import com.deweyvm.dogue.common.logging.Log
import java.io.IOException
import java.net.{SocketException, UnknownHostException}
import com.deweyvm.dogue.entities.Code
import com.deweyvm.dogue.common.Implicits._
import com.deweyvm.dogue.common.protocol.DogueMessage
import com.deweyvm.dogue.common.io.DogueSocket

object ClientManager {
  var num = 0
}

class ClientManager(port:Int, host:String) extends Task with Transmitter[DogueMessage] {
  //result type of actions (success, failure). should probably be Either
  type T = Unit
  private val clientName = {
    val u = Game.settings.username
    if (u == null) createName else u
  }

  var destName:String = "&unknown&"

  override def sourceName = clientName
  override def destinationName = destName
  private var state:ClientState = Client.State.Connecting
  var client:Option[Client] = None

  private def createName = {
    Game.globals.makeMiniGuid
  }

  private def tryConnect() {
    def fail(exc:Exception, error:String => ClientError) {
      val stackTrace = exc.getStackTraceString
      Log.warn(Log.formatStackTrace(exc))
      delete(error(stackTrace).toState)
      Thread.sleep(5000)
    }
    try {
      def callback(socket:DogueSocket, serverName:String) {
        client = new Client(clientName, serverName, socket, this).some
        destName = serverName
        state = Client.State.Connected
        Log.all("Handshake succeeded")
      }
      if (state != Client.State.Handshaking && state != Client.State.Closed) {
        Log.info("Attempting to establish a connection to %s" format host)
        ClientManager.num += 1
        new DogueHandshake(clientName, host, port, callback).start()
        state = Client.State.Handshaking
      } else {
        Thread.sleep(100)
      }
      ()
    } catch {
      case ioe:IOException =>
        fail(ioe, Client.Error.ConnectionFailure)
      case uhe:UnknownHostException =>
        fail(uhe, Client.Error.HostUnreachable)
    }
  }

  private def delete(s:ClientState) {
    try {
      Log.info("Deleting client")
      tryMap {_.close()}
      client = None
      state = s
    } catch {
      case t:Throwable => ()
    }
  }

  override def doWork() {
    if (!client.isDefined) {
      tryConnect()
    }
    tryMap { _.run() }
  }

  override def cleanup() {
    Log.info("ClientManager died")
  }


  override def enqueue(s:DogueMessage) {
    tryMap { _.enqueue(s) }

  }

  override def dequeue:Vector[DogueMessage] = {
    tryMap {_.dequeue} getOrElse Vector()
  }


  def disconnect(reason:ClientState) {
    tryMap {_.close()}
    delete(reason)
  }

  def doTimeout() {
    Log.warn("ping timeout")
    disconnect(Client.Error.Timeout.toState)
  }


  private def tryMap[A](f:Client => A/*,r: A => T*/):Option[A] = {
    import Client.Error._
    try {
      client map f
    } catch {
      case e:SocketException =>
        delete(ConnectionFailure(e.getMessage).toState)
        None
    }
  }

  def sendPing() {
    tryMap(_.sendPing())
  }


  def getStatus:String = {
    import Client.State._
    import Client.Error._
    state match {
      case Offline => "Offline mode"
      case Connected => Code.☼.rawString
      case Handshaking => "Handshaking..."
      case Connecting =>
        val codes = Vector(Code./, Code.─, Code.\, Code.│)
        "Connecting " + codes((Game.getFrame/10) % codes.length).rawString
      case Disconnected(e) => e match {
        case HostUnreachable(msg) => "Server is down (r)" //todo -- put control widget here
        case ConnectionFailure(msg) => "Failed to connect (r)" //todo -- put control widget here
        case Unknown => "Uknown error"
        case Timeout => "Ping timeout"
      }
      case Closed => "Closing..."
    }
  }

  /**
   * Alert the server that the client application is closing.
   */
  def close() {
    tryMap {_.close()}
    delete(Client.State.Closed)
    kill()
  }
}
