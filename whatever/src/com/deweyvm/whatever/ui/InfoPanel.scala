package com.deweyvm.whatever.ui

import com.deweyvm.whatever.graphics.GlyphFactory
import com.deweyvm.gleany.graphics.Color
import com.deweyvm.gleany.GleanyMath
import com.deweyvm.whatever.input.Controls

object InfoPanel {
  def makeNew(x:Int, y:Int, width:Int, height:Int, factory:GlyphFactory):InfoPanel = {
    new InfoPanel(x, y, width, height, factory, "", Vector(), new ScrollBar(factory), 0)
  }

  def splitText(string:String, textWidth:Int):Vector[String] = {
    val (last, lines) = string.foldLeft(("", Vector[String]())){
      case ((currentLine, lines), c) =>
        val added = currentLine + c
        if (added.length == textWidth - 1) {
          val hyphen = if (c == ' ') "" else  "-"
          ("", lines ++ Vector(added + hyphen))
        } else {
          (added, lines)
        }
    }
    lines ++ Vector(last)
  }
}

case class InfoPanel(override val x:Int,
                     override val y:Int,
                     override val width:Int,
                     override val height:Int,
                     factory:GlyphFactory,
                     text:String,
                     lines:Vector[Text],
                     scrollBar:ScrollBar,
                     jView:Int,
                     ctr:Int=0)
  extends Panel(x, y, width, height) {
  private val leftMargin = 0
  private val rightMargin = 1
  private val topMargin = 0
  private val textWidth = width - leftMargin - rightMargin

  def addText(string:String, bgColor:Color, fgColor:Color):InfoPanel = {
    val addedLines = InfoPanel.splitText(string, textWidth) map { s =>
      new Text(s, bgColor, fgColor, factory)
    }
    this.copy(text = text + string,
              lines = lines ++ addedLines)
  }

  private def updateView:InfoPanel = {
    val jMax = lines.length - 1
    val jMin = height - 1
    this.copy(jView = GleanyMath.clamp(-Controls.AxisY.justPressed + jView, jMin, jMax))
  }

  override def update():InfoPanel = {
    val (addLine, newCtr) =
      if (ctr >= 180) {
        (true, 0)
      } else {
        (false, ctr + 1)
      }

    val next = if (addLine) {
      this.addText("this is an added line", Color.White, Color.Black)
    } else {
      this
    }
    next.copy(ctr = newCtr).updateView
  }

  override def draw() {
    super.draw()
    drawLines(lines, width, height, leftMargin + x, topMargin + y)
  }

  def drawText(text:Text, i:Int, j:Int) {
    text.filterDraw(i, j, contains)
  }

  private def drawLines(lines:Vector[Text], width:Int, height:Int, iRoot:Int, jRoot:Int) {
    scrollBar.draw(lines.length, lines.length - jView - 1, width, height, iRoot, jRoot)
    for (k <- 0 until height) {
      val jj = (k + (lines.length - jView - 1))
      if (jj >= 0 && jj < lines.length) {
        val line = lines(jj)
        line.draw(iRoot, jRoot + k)
      }
    }
  }
}


