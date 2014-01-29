package com.deweyvm.dogue.ui

import com.deweyvm.dogue.graphics.GlyphFactory
import com.deweyvm.gleany.graphics.Color
import com.deweyvm.dogue.input.Controls
import com.deweyvm.dogue.common.Implicits._
import com.deweyvm.dogue.common.logging.Log

object InfoPanel {
  def makeNew(x:Int, y:Int, width:Int, height:Int, bgColor:Color, factory:GlyphFactory):InfoPanel = {
    new InfoPanel(x, y, width, height, bgColor, factory, "", Vector(), new ScrollBar(factory), 0)
  }


}

case class InfoPanel(override val x:Int,
                     override val y:Int,
                     override val width:Int,
                     override val height:Int,
                     bgColor:Color,
                     factory:GlyphFactory,
                     text:String,
                     lines:Vector[Text],
                     scrollBar:ScrollBar,
                     jView:Int)
  extends Panel(x, y, width, height, bgColor) {
  private val leftMargin = 0
  private val rightMargin = 1
  private val topMargin = 0
  private val textWidth = width - leftMargin - rightMargin

  def addText(string:String, bgColor:Color, fgColor:Color):InfoPanel = {
    val addedLines = string.toLines(textWidth) map { s =>
      new Text(s, bgColor, fgColor, factory)
    }
    this.copy(text = text + string,
              lines = lines ++ addedLines)
  }

  private def updateView:InfoPanel = {
    val (jMin, jMax) = {
      if (lines.length < height) {
        (lines.length - 1, height - 1)
      } else {
        (height - 1, lines.length - 1)
      }
    }
    this.copy(jView = (-Controls.AxisY.justPressed + jView).clamp(jMin, jMax))
  }

  override def update:InfoPanel = {
    this.updateView
  }

  override def draw() {
    super.draw()
    drawText()
  }

  def drawText() {
    drawLines(lines, width, height, leftMargin + x, topMargin + y)
  }

  def drawText(text:Text, i:Int, j:Int) {
    text.filterDraw(i, j, contains)
  }

  private def drawLines(lines:Vector[Text], width:Int, height:Int, iRoot:Int, jRoot:Int) {
    scrollBar.draw(lines.length, lines.length - jView - 1, width, height, iRoot, jRoot)
    for (k <- 0 until height) {
      val jj = k + (lines.length - jView - 1)
      if (jj >= 0 && jj < lines.length) {
        val line = lines(jj)
        line.draw(iRoot, jRoot + k)
      }
    }
  }
}


