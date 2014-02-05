package com.deweyvm.dogue.common.parsing

import scala.util.parsing.combinator.RegexParsers

import com.deweyvm.dogue.common.protocol._
import com.deweyvm.dogue.common.protocol.Invalid
import org.scalacheck.{Prop, Gen, Arbitrary}
import com.deweyvm.dogue.common.data.Encoding
import com.deweyvm.dogue.common.logging.Log
import scala.util.Random
import org.scalacheck.Test.Parameters.Default
import org.scalacheck.Test.TestCallback
import org.scalacheck.Test.Result
import org.scalacheck.util.ConsoleReporter


object CommandParser {
  val badChars = Set[Char](' ', '\\', '"')
  import Gen.{choose, frequency, listOf, resize, const, oneOf}
  def lowerCodes = choose(1.toChar, 31.toChar)
  //excludes double quote, backslash, space
  def goodAscii = oneOf(frequency((1, const(33))),
                        frequency((56, choose(35.toChar, 91.toChar))),
                        frequency((33, choose(93.toChar, 126.toChar))))

  def validChars = goodAscii
  def nonQuote = resize(10, Gen.nonEmptyListOf(validChars).map(_.mkString))
  def quote = nonQuote.map({ (x: String) =>
    val randIndex = x.length
    x.toVector.patch(Random.nextInt(randIndex), Vector(' '), 0).mkString
  })
  def arg = quote//oneOf(nonQuote, quote)
  implicit def goodCommand:Arbitrary[(Command, String)] = {
    Arbitrary {
      for {
        op <- oneOf(DogueOps.getAll)
        src <- nonQuote
        dst <- nonQuote
        args <- listOf(arg)
      } yield {
        val first = "%s %s %s" format (op.toString, src, dst)
        val mappedArg = args.map(x =>
          if (x.contains(' ')) {
            "\"" + x + "\""
          } else {
            x
          }
        )
        val rest =
          if (mappedArg.length > 0) {
            " " + mappedArg.mkString(" ")
          } else {
            ""
          }
        val result = (Command(op, src, dst, args.toVector), first + rest)
        assert(result._1.toString == result._2)
        result
      }

    }
  }
  def test() {



    def parseAssert[T](example:(Command,String), lhs:Command=>T, rhs:Command => T):Boolean = {
      example match {
        case (command, input) =>
          val parser = new CommandParser
          val parsed = parser.getCommand(input)
          parsed match {
            case Invalid(input, msg) =>
              System.out.println("Failed to parse: %s" format input)
              System.out.flush()
              false
            case p@Command(_,_,_,_) =>
              val l = lhs(p)
              val r = rhs(command)
              val result = l == r
              if (!result) {
                System.out.println("%s: \n[PARSE]%s != \n[GIVEN]%s" format (command.toString, l, r))
                System.out.flush()
              }
              result
          }
      }


    }
    //parseAssert((Command(DogueOps.Say, "'33S]33~333333", "e3333|33pB33", Vector("333333 B33zR")), "say '33S]33~333333 e3333|33pB33 \"333333 B33zR\""), _.dest, _.dest)
    //return
    val opMatch = Prop.forAll({ r:(Command, String) =>
      parseAssert(r, _.op, _.op)
    }).label("Parsed command op match")

    val argLengthMatch = Prop.forAll({ r:(Command, String) =>
      parseAssert(r, _.args.length, _.args.length)
    }).label("Parsed command arg length match")

    val sourceMatch = Prop.forAll({ r:(Command, String) =>
      parseAssert(r, _.source, _.source)
    }).label("Parsed command source match")

    val destMatch = Prop.forAll({ r:(Command, String) =>
      parseAssert(r, _.dest, _.dest)
    }).label("Parsed command dest match")


    val commandMatch = Prop.forAll({ r:(Command, String) =>
      parseAssert(r, _.toString, _.toString)
    }).label("Parsed command string match")

    (opMatch && sourceMatch && destMatch && argLengthMatch && commandMatch).check(new Default{}.
      withWorkers(100).
      withTestCallback(new ConsoleReporter(1) {
          override def onTestResult(name: String, result: Result) {
            if (!result.passed) {
              super.onTestResult(name, result)
            } else {

            }
          }
    }))
    //Test.runScalaCheck(/*argLengthMatch &&*/ opMatch /*&& sourceMatch && destMatch && commandMatch*/)

  }
}

class CommandParser extends RegexParsers {
  override type Elem = Char

  def parseOp = opChoices<~"""(?!\w)""".r
  def opChoices = sayOp      |
                  pingOp     |
                  pongOp     |
                  greetOp    |
                  closeOp    |
                  nickOp     |
                  reassignOp |
                  identifyOp |
                  assignOp   |
                  localOp
  def sayOp      = "say".r      ^^ { _ => DogueOps.Say }
  def pingOp     = "ping".r     ^^ { _ => DogueOps.Ping }
  def pongOp     = "pong".r     ^^ { _ => DogueOps.Pong }
  def greetOp    = "greet".r    ^^ { _ => DogueOps.Greet }
  def closeOp    = "close".r    ^^ { _ => DogueOps.Close }
  def nickOp     = "nick".r     ^^ { _ => DogueOps.Nick }
  def reassignOp = "reassign".r ^^ { _ => DogueOps.Reassign }
  def identifyOp = "identify".r ^^ { _ => DogueOps.Identify }
  def assignOp   = "assign".r   ^^ { _ => DogueOps.Assign }
  def localOp    = "local".r    ^^ { _ => DogueOps.LocalMessage }
  def parseArg = """[^\s\x{0}"]+""".r
  def parseWord = parseString | parseArg //"""\w+""".r
  def parseString = """"[^"]*"""".r ^^ { x => x.substring(1, x.length - 1)}
  def parseArgs = rep1(parseWord)
  def parseCommand: Parser[DogueMessage] = parseWord~parseWord~parseWord~parseArgs.? ^^ {  case rawOp~src~dest~args =>
    getOp(rawOp) match {
      case Right(op) => Command(op, src, dest, args map {_.toVector} getOrElse Vector())
      case Left((in, msg)) => Invalid(in, msg)
    }

  }

  def parseLocalCommand:Parser[LocalMessage] = parseWord~parseArgs.? ^^ { case rawOp~args =>
    getOp(rawOp) match {
      case Right(op) => LocalCommand(op, args map {_.toVector} getOrElse Vector())
      case Left((in, msg)) => LocalInvalid(in, msg)
    }
  }

  def getLocalCommand(input:String):LocalMessage = {
    val parseResult = parse(parseLocalCommand, input)
    parseResult.getOrElse(LocalInvalid(input, parseResult.toString))
  }

  def getCommand(input:String):DogueMessage = {
    val parseResult = parse(parseCommand, input)
    parseResult.getOrElse(Invalid(input, parseResult.toString))
  }

  def getOp(input:String):Either[(String, String), DogueOp] = {
    val parseResult = parse(parseOp, input)
    if (parseResult.successful) {
      Right(parseResult.get)
    } else {
      Left((input, parseResult.toString))
    }
  }
}
