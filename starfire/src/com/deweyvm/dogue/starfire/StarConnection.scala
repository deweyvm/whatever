package com.deweyvm.dogue.starfire

import java.net.Socket
import scala.collection.mutable.ArrayBuffer
import com.deweyvm.dogue.common.Implicits._
import com.deweyvm.dogue.common.logging.Log
import com.deweyvm.dogue.common.threading.{ThreadManager, Task}
import com.deweyvm.dogue.common.io.DogueSocket
import com.deweyvm.dogue.common.protocol.{Invalid, Command, DogueMessage}


class StarConnection(clientName:String, socket:DogueSocket, parent:Starfire, id:Int) extends Task {
  def getName = parent.name
  private val ponger = ThreadManager.spawn(new StarPonger(this))
  socket.setTimeout(500)

  override def killAux() {
    ponger.kill()
  }

  override def cleanup() {
    Log.info("Reader closed")
    ponger.kill()
  }

  def write(s:DogueMessage) {
    socket.transmit(s)
  }

  override def doWork() {
    val commands = socket.receiveCommands()
    commands foreach {
      case inv@Invalid(_,_) =>
        inv.warn()
      case cmd@Command(_,_,_,_) =>
        new StarWorker(cmd, this, socket).start()
    }
    Thread.sleep(500)
  }

  def pong() {
    ponger.pong()
  }

  def broadcast(from:String, string:String) {
    parent.broadcast(from, string)
  }

}


