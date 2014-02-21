package com.deweyvm.dogue.graphics.visualizers

import com.deweyvm.dogue.graphics.OglRenderer
import com.deweyvm.dogue.common.data.{Code, Array2d}
import com.deweyvm.gleany.data.Point2d
import com.deweyvm.dogue.common.procgen._
import com.deweyvm.gleany.graphics.Color
import com.deweyvm.dogue.common.Implicits
import Implicits._
import scala.util.Random
import com.deweyvm.dogue.input.Controls
import com.deweyvm.dogue.entities.Tile
import com.badlogic.gdx.Gdx
import com.deweyvm.dogue.{Dogue, Game}


class HexGridVisualizer {
  val cols = 49
  val rows = 49
  var seed = 0
  val hexSize = 16
  var hexGrid = makeHexGrid
  var cursorX = 0
  var cursorY = 0

  val dx = 100
  val dy = 30

  def mx:Int = Dogue.gdxInput.map{_.getX}.getOrElse(0) - dx
  def my:Int = Dogue.gdxInput.map{_.getY}.getOrElse(0) - dy
  def mouse = Point2d(mx*3.25, my*1.20)

  def makeHexGrid = new HexGrid(hexSize, cols, rows, 0, seed)

  def batchDraw(r:OglRenderer) {
    hexGrid.graph.nodes.foreach { node =>
      val poly = node.self
      val upLeft = poly.upperLeft
      upLeft foreach { pt =>
        val c = getColor(node.getNeighbors.length)
        val code = Code.unicodeToCode((node.getNeighbors.length + 48).toChar)
        val tile = new Tile(code, Color.Blank, c)
        r.drawTileRaw(tile, pt.x + dx - 3, pt.y + dy + 2)
      }
    }
  }

  def render(r:OglRenderer) {
    cursorX = (cursorX + Controls.AxisX.zip(0, 3)).clamp(0, hexGrid.hexCols)
    cursorY = (cursorY + Controls.AxisY.zip(0, 3)).clamp(0, hexGrid.hexRows)
    if (Controls.Space.justPressed) {
      seed += 1
      hexGrid = makeHexGrid
    }

    r.translateShape(dx, dy){ () =>
      hexGrid.hexes foreach { case (i, j, h) => ()
        r.drawPoint(h, Color.White, 1)
      }
      hexGrid.graph.nodes.foreach { node =>
        val poly = node.self
        val c = getColor(node.getNeighbors.length)
        r.drawPolygon(poly, c)
      }
      //val m = mouse
      val x = mx/hexSize
      val yOffset = x.isOdd.select(-hexSize/2, 0)
      val y = ((3.0/4.0)*(my + yOffset)/hexSize).toInt
      val k = Hex.coordsToIndex(x, y, hexGrid.hexCols)
      //println("(%d, %d) => %d" format (x, y, k))
      val len = hexGrid.polys.length
      if (k >= 0 && k <= len - 1) {
        hexGrid.polys(k).foreach { poly =>
          r.drawPolygon(poly, Color.White)
        }
      }

    }


  }

  def getColor(i:Int) = i match {
    case 1 => Color.Blue
    case 2 => Color.Green
    case 3 => Color.Yellow
    case 4 => Color.Orange
    case 5 => Color.Red
    case 6 => Color.Purple
    case _ => Color.White
  }
}
