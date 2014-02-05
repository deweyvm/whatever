package com.deweyvm.dogue.entities

import com.deweyvm.gleany.graphics.Color
import com.deweyvm.dogue.graphics.{GlyphFactory, RectSprite, Glyph}
import com.deweyvm.dogue.Assets
import com.deweyvm.dogue.common.data.Code


object Tile {
  def fromCode(code:Code, bgColor:Color, fgColor:Color, factory:GlyphFactory) = {
    new Tile(bgColor, fgColor, code.index, factory)
  }
}

class Tile(bgColor:Color, fgColor:Color, index:Int, factory:GlyphFactory) {


  val width = factory.tileWidth
  val height = factory.tileHeight
  val character = factory.makeGlyph(index, fgColor)

  val bg = new RectSprite(width, height, bgColor)

  def draw(i:Int, j:Int) {
    val x = i*width
    val y = j*height
    bg.draw(x, y)
    character.draw(x, y)
  }
}
