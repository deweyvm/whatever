package com.deweyvm.dogue.common.protocol

import com.deweyvm.dogue.common.parsing.CommandParser

object Command {

  def test() {
    val parser = new CommandParser
    val tests = List(
      ("test from to a b c", false),
      ("/test from to a b c", true),
      ("/test from                  \tto a b c", true),
      ("/test from to a", true),
      ("/test a", false),
      ("/ a b c d", false),
      ("/t a b", true),
      ("/say 6bdaeba28f26b3e3 6bdaeba28f26b3e3 ?", true),
      ("/say 5e01405ec801cfa4 5e01405ec801cfa4 HUH?", true),
      ("/greet flare &unknown& identify", true)

    )



    def parse(s:String) = parser.parseAll(parser.command, s)

    tests foreach { case (s, expected) =>
      val parsed = parse(s)
      val index = (tests map {_._1}).indexOf(s)
      assert (parsed.successful == expected,  index + " " + s + "\n" + parsed)
      parser.parseToOpt(parsed) foreach { p =>
        assert(p.toString == s.replaceAll("\\s+", " "), "\"%s\" != \"%s\"" format (p.toString, s))
      }
    }
  }
}

trait DogueMessage
case class Command(op:String, source:String, dest:String, args:Vector[String]) extends DogueMessage {
  override def toString:String = {
    val front = "/%s %s %s" format (op, source, dest)
    if (args.length == 0) {
      front
    } else {
      "%s %s" format (front, args.mkString(" "))
    }
  }

  def this(op:String, source:String, dest:String, args:String*) =
    this(op, source, dest, args.toVector)

  def toSay:String = {
    args.mkString(" ")
  }
}

case class Invalid(msg:String) extends DogueMessage
