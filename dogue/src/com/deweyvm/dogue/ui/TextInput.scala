package com.deweyvm.dogue.ui

import com.badlogic.gdx.{InputAdapter, Gdx}
import com.deweyvm.dogue.graphics.GlyphFactory
import com.deweyvm.dogue.common.Implicits._
import com.deweyvm.gleany.graphics.Color
import com.deweyvm.dogue.Game
import com.deweyvm.dogue.common.protocol.{DogueOp, Invalid, DogueMessage, Command}
import com.deweyvm.dogue.net.{Client, Transmitter}
import com.deweyvm.dogue.common.logging.Log
import com.deweyvm.dogue.common.threading.Lock
import com.deweyvm.dogue.common.parsing.{ParseError, CommandParser}


object TextInput {
  private val parser = new CommandParser
  private var count = 0
  private val lock = new Lock
  def create(prompt:String, width:Int, height:Int, bgColor:Color, fgColor:Color, factory:GlyphFactory):TextInput = {
    val result = new TextInput(count, prompt, width, height, bgColor, fgColor, "", factory)
    count += 1
    result
  }

  def addListener() {

    Gdx.input.setInputProcessor(new InputAdapter {
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
            //println("here <%s>".format(char.toInt.toString))
          }
        }
        false
      }
    })

  }

  //pure reads do not need a lock as long as all writes are locked
  def take(id:Int):String = {
    val added = strings(id)
    added
  }

  val strings = scala.collection.mutable.Map[Int, String]().withDefaultValue("")
  val inputs = scala.collection.mutable.Map[Int, TextInput]()
  val commandQueue = scala.collection.mutable.Map[Int, Vector[String]]().withDefaultValue(Vector())

  def putCommand(id:Int, string:String) {
    type T = Unit ;
    lock.foreach[(Int,String)]({case (i,s) =>
      val newVect:Vector[String] = commandQueue(i) :+ s
      commandQueue(i) = newVect

    })((id, string))

  }

  def getCommands(id:Int, transmitter:Transmitter[DogueMessage]):Vector[DogueMessage] = {
    lock.get({ () =>
      val result = (commandQueue(id).toVector map lineToCommand(transmitter)).flatten
      commandQueue(id) = Vector()
      result
    })

  }

  /**
   * return None if the command is executed immediately on the client as per #76
   * @param transmitter
   * @param line
   * @return
   */
  def lineToCommand(transmitter:Transmitter[DogueMessage])(line:String):Option[DogueMessage] = {
    Log.all("Converting \"%s\" to command for server" + line)
    try {
      val source = transmitter.sourceName
      val dest = transmitter.destinationName
      val (op, rest) = if (line(0) == '/') {
        val command = line.drop(1)
        val rest = line.split(" ", 2).toVector tryGet 1 getOrElse ""
        val op = parser.getOp(command)

        (op, rest)
      } else {
        (DogueOp.Say, line)
      }
      val result = Command(op, source, dest, Vector(rest))
      op match {
        case DogueOp.Quit =>
          Log.info("Quit command")
          Game.shutdown()
          None
        case _ =>
          result.some
      }


    } catch {
      case t:ParseError =>
        Log.warn(Log.formatStackTrace(t))
        Invalid(line, t.getMessage).some
    }
  }


  var active:Option[Int] = None

}

case class TextInput(id:Int, prompt:String, width:Int, height:Int, bgColor:Color, fgColor:Color, string:String, factory:GlyphFactory) {
  TextInput.inputs(id) = this

  private def makeText(s:String) = {
    new Text(s, bgColor, fgColor, factory)
  }


  val text = (prompt + string).toLines(width) map makeText
  val cursor = Vector(makeText("_"), makeText(" "))
  val flashRate = 30 //cursor flashes on/off for `flashRate` frames

  def update(transmitter:Transmitter[DogueMessage]):(TextInput, Vector[DogueMessage]) = {
    TextInput.active = Some(id)
    val commands = TextInput.getCommands(id, transmitter)
    (this.copy(string = TextInput.take(id)), commands)
  }

  def draw(iRoot:Int, jRoot:Int) {
    val iStart = scala.math.max(0, text.length - height)
    val end = scala.math.min(text.length, height)
    for (i <- 0 until end) {
      text(i + iStart).draw(iRoot, jRoot + i)
    }
    if (text.length >= 0 && text(0).width > 0)  {
      cursor(Game.getFrame/flashRate % 2).draw(iRoot+text(text.length - 1).width, jRoot + end - 1)
    }
  }
}
