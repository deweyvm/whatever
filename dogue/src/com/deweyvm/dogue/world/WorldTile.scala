package com.deweyvm.dogue.world

import com.deweyvm.dogue.entities.Tile
import com.deweyvm.gleany.graphics.Color
import com.deweyvm.gleany.data.Point2d
import com.deweyvm.dogue.world.biomes.{Biomes, Biome}
import com.deweyvm.dogue.ui.world.Tooltip
import com.deweyvm.dogue.DogueImplicits._

object WorldTile {
  val Blank = WorldTile(0 m, Altitude.Void, SurfaceType.Void, 1 atm, 0, Biomes.Void, Color.Black, Latitude.Void, Point2d.UnitX, 0, 0,  Summer, Tile.Blank)
}

case class WorldTile(height:Meters,
                     altitude:AltitudinalRegion,
                     surface:SurfaceType,
                     pressure:Pressure,
                     moisture:Rainfall,
                     biome:Biome,
                     biomeColor:Color,
                     latitude:LatitudinalRegion,
                     wind:Point2d,
                     daylight:Double,
                     sunTemp:Celcius,
                     season:Season,
                     tile:Tile) {
  def fullTooltip:Tooltip = Tooltip(Color.White, Vector(
    "Height   : %.2fm" format height.d,
    "Altitude : %s" format altitude,
    "Region   : %s" format biome,
    "Surface  : %s" format surface,
    "Latitude : %s" format latitude,
    "Wind     : %.2fm/s" format wind.magnitude,
    "Daylight : %.2f" format daylight,
    "Mean Temp: %.2fC" format sunTemp.d,
    "Pressure : %.2fatm" format pressure.d,
    "Rainfall : %.2fmm/yr" format moisture.d,
    "",
    "Season: %s" format season.toString
  ))

  def regionTooltip:Tooltip = fullTooltip//.copy(color = Color.White)
}
