package com.deweyvm.dogue.ui

import com.badlogic.gdx.InputAdapter
import com.deweyvm.dogue.common.CommonImplicits._
import com.deweyvm.gleany.graphics.Color
import com.deweyvm.dogue.{Dogue, Game}
import com.deweyvm.dogue.common.protocol._
import com.deweyvm.dogue.net.{Client, Transmitter}
import com.deweyvm.dogue.common.logging.Log
import com.deweyvm.dogue.common.parsing.CommandParser
import scala.Some
import com.deweyvm.dogue.graphics.WindowRenderer
import scala.collection.immutable.IndexedSeq


object TextInput {
  var active:Option[String] = None
  val chat = "chat"
  private val parser = new CommandParser
  def getPrompt = Client.name + ": "
  def create(name:String, width:Int, height:Int, bgColor:Color, fgColor:Color):TextInput = {

    val result = new TextInput(name, getPrompt, width, height, bgColor, fgColor, "")
    result
  }

  def addListener() {
    Dogue.gdxInput foreach {
      _.setInputProcessor(new InputAdapter {
        override def keyTyped(char:Char):Boolean = {
          //todo, make a collective reference where you can just query .string, .queue, etc
          val code = char.toInt
          active foreach { id =>
            if (code == 8) {
              strings(id) = strings(id).dropRight(1)
            } else if (code == 13 && strings(id).length > 0) {
              putCommand(id, strings(id))
              strings(id) = ""
            } else if (code > 31){
              strings(id) += char
            }
          }
          false
        }
      })
    }

  }

  //pure reads do not need a lock as long as all writes are locked
  def take(id:String):String = {
    val added = strings(id)
    added
  }

  val strings = scala.collection.mutable.Map[String, String]().withDefaultValue("")
  val inputs = scala.collection.mutable.Map[String, TextInput]()
  val commandQueue = scala.collection.mutable.Map[String, Vector[String]]().withDefaultValue(Vector())

  def putCommand(id:String, string:String):Unit = synchronized {
    val newVect:Vector[String] = commandQueue(id) :+ string
    commandQueue(id) = newVect
  }

  def prependCommand(id:String, string:String):Unit = synchronized {
    val newVect:Vector[String] = string +: commandQueue(id)
    commandQueue(id) = newVect
  }

  def getCommands(id:String, transmitter:Transmitter[DogueMessage]):(Vector[DogueMessage]) = synchronized {
    val result = (commandQueue(id).toVector map lineToCommands(transmitter)).flatten
    commandQueue(id) = Vector()
    result
  }

  /**
   * Return None if the command is executed immediately.
   * Otherwise return the commands to be sent to the server.
   */
  def lineToCommands(transmitter:Transmitter[DogueMessage])(line:String):Vector[DogueMessage] = {
    Log.all("Converting \"%s\" to command" format line)
    val source = transmitter.sourceName
    val dest = transmitter.destinationName
    val command =
      if (line(0) == '/') {
        line.drop(1)
      } else {
        "say %s \"%s\"" format (source, line)
      }
    val parsed = parser.getLocalCommand(command)
    def makeLocal(s:String) = {
      new LocalCommand(DogueOps.LocalMessage, s).toDogueMessage(source, dest)
    }
    //fixme issue #100
    parsed match {
      case cmd@LocalCommand(op, args) =>
        op match {
          case DogueOps.Connect =>
            if (Client.instance.requestConnect()) {
              Vector()//reconnecting already shows a local message
            } else {
              Vector(makeLocal("You are already connected or attempting to connect"))
            }
          case DogueOps.Close =>
            Log.info("Quit command")
            Game.shutdown()
            Vector()
          case DogueOps.Register =>
            if (Game.settings.password.get != "") {
              Vector(makeLocal("You are already registered -- don't be greedy!"))
            } else {
              Log.info("Attempting to register username")
              val registerMessage = parsed.toDogueMessage(source, dest)
              val chatOutput = makeLocal("Attempting to register name")
              Vector(registerMessage, chatOutput)
            }
          case _ =>
            Vector(parsed.toDogueMessage(source, dest))
        }
      case inv@LocalInvalid(s, msg) =>
        Vector(new LocalCommand(DogueOps.LocalMessage, "Invalid command.").toDogueMessage(source, dest))
    }


  }
}

case class TextInput(id:String, prompt:String, width:Int, height:Int, bgColor:Color, fgColor:Color, string:String) {
  TextInput.inputs(id) = this

  private def makeText(s:String) = {
    Text.fromString(bgColor, fgColor)(s)
  }


  val text = (prompt + string).toLines(width) map makeText
  val cursor = Vector(makeText("_"), makeText(" "))
  val flashRate = 30 //cursor flashes on/off for `flashRate` frames

  def update(transmitter:Transmitter[DogueMessage]):(TextInput, Vector[DogueMessage]) = {
    TextInput.active = Some(id)
    val serverCommands = TextInput.getCommands(id, transmitter)
    (this.copy(prompt=TextInput.getPrompt, string = TextInput.take(id)), serverCommands)
  }

  def drawCursor(iRoot:Int, jRoot:Int, end:Int)(r:WindowRenderer):WindowRenderer = {
    (text.length >= 0 && text(0).width > 0).partial(
      cursor(Game.getFrame/flashRate % 2).draw(iRoot+text(text.length - 1).width, jRoot + end - 1)(r)
    ).getOrElse(r)
  }

  def draw(iRoot:Int, jRoot:Int)(r:WindowRenderer):WindowRenderer = {
    val iStart = math.max(0, text.length - height)
    val end = math.min(text.length, height)
    val lines = for (i <- 0 until end) yield {
      text(i + iStart).draw(iRoot, jRoot + i) _
    }
    r <++| lines <+| drawCursor(iRoot, jRoot, end)
  }
}
