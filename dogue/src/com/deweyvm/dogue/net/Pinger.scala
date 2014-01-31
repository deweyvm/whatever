package com.deweyvm.dogue.net

import com.deweyvm.dogue.Game
import com.deweyvm.dogue.common.threading.Task
import com.deweyvm.dogue.common.logging.Log

class Pinger(clientManager:ClientManager) extends Task {
  private var lastPongReceived = Game.getFrame
  private var lastPingSent = Game.getFrame
  private val pingFrequency = 5*60
  private val maxPingFrames = 120*60
  private val checkFrequency = 350
  override def doWork() {
    Thread.sleep(checkFrequency)
    if (Game.getFrame - lastPingSent > pingFrequency) {
      Log.verbose("Sending ping " + Game.getFrame)
      lastPingSent = Game.getFrame
      clientManager.sendPing()
    }
    if (Game.getFrame - lastPongReceived > maxPingFrames) {
      clientManager.doTimeout()
      Log.info("Ping timeout %d seconds" format (maxPingFrames/60))
      kill()
    }
  }

  override def cleanup() {
    Log.info("Pinger shutting down")
  }

  def pong() {
    lastPongReceived = Game.getFrame
  }
}
