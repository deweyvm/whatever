package com.deweyvm.dogue.world.biomes

import com.deweyvm.gleany.graphics.Color
import com.deweyvm.dogue.common.data.{DogueRange, Code}
import com.deweyvm.dogue.world._
import com.deweyvm.dogue.common.reflect.Reflection
import com.deweyvm.dogue.DogueImplicits
import DogueImplicits._
import com.deweyvm.dogue.common.Implicits
import Implicits._

trait BiomeType {
  val baseColor:Color
  val code:Code
}
object BiomeType {
  case object GrasslandScrublandSavanna extends BiomeType {
    val baseColor = Color.Green
    val code = Code.`»`
  }
  case object Forest extends BiomeType {
    val baseColor = Color.DarkGreen
    val code = Code.♠
  }
  case object Desert extends BiomeType {
    val baseColor = Color.Tan
    val code = Code.`.`
  }
  case object Wetlands extends BiomeType {
    val baseColor = Color.Brown
    val code = Code.~
  }
  case object Alpine extends BiomeType {
    val baseColor = Color.Grey
    val code = Code.▲
  }
  case object Aquatic extends BiomeType {
    val baseColor = Color.Blue
    val code = Code.≈
  }

  case object Special extends BiomeType {
    val baseColor = Color.Purple
    val code = Code.`¢`
  }
}

object Biomes {

  def LandBiome(name:String,
                `type`:BiomeType,
                region:DogueRange[LatitudinalRegion],
                moisture:DogueRange[Rainfall],
                altitude:DogueRange[AltitudinalRegion]) = {
    val spec = BiomeSpec(Surface.Land, `type`, region, moisture, altitude)
    Biome(name, spec)
  }

  def AquaticBiome(name:String,
                   `type`:BiomeType,
                   region:DogueRange[LatitudinalRegion],
                   moisture:DogueRange[Rainfall],
                   altitude:DogueRange[AltitudinalRegion]) = {
    val spec = BiomeSpec(Surface.Water, `type`, region, moisture, altitude)
    Biome(name, spec)
  }

  def getBiome(wet:Rainfall, temp:Double, lat:LatitudinalRegion, alt:AltitudinalRegion, surf:SurfaceType):Biome = {
    val found = All.filter { b =>
      b.spec.surface == surf &&
      b.spec.region.contains(lat) &&
      b.spec.moisture.contains(wet) &&
      b.spec.altitude.contains(alt)
    }
    if (found.length > 1) {

      println("(%s) found for :" format found.mkString(","))
      println("    Moisture : %.2f" format wet.d)
      println("    Latitude : %s" format lat)
      println("    Altitude : %s" format alt)
      println("    Surface  : %s" format surf)
    }

    found.headOption.getOrElse {
      println("Void region:")
      println("    Moisture : %.2f" format wet.d)
      println("    Latitude : %s" format lat)
      println("    Altitude : %s" format alt)
      println("    Surface  : %s" format surf)
      throw new RuntimeException()
      Void
    }
  }

  import Latitude._
  import Altitude._
  val Void = LandBiome("Void",
    BiomeType.Special,
    SuperTropical <=> Polar,
    0.`mm/yr` <=> 10000.`mm/yr`,
    Abyss <=> SuperAlpine
  )

  val Lake = AquaticBiome("Lake",
    BiomeType.Aquatic,
    SuperTropical <=> Polar,
    0.`mm/yr` <=> 10000.`mm/yr`,
    Lowlands <=> SuperAlpine
  )

  val Ocean = AquaticBiome("Ocean",
    BiomeType.Aquatic,
    SuperTropical <=> Polar,
    0.`mm/yr` <=> 10000.`mm/yr`,
    Abyss <=> Oceanic
  )

  val PolarDesert = LandBiome("Polar Desert",
    BiomeType.Desert,
    Subpolar <=> Polar,
    0.`mm/yr` <=> 250.`mm/yr`,
    Lowlands <=> Montane
  )

  val FrozenCrags = LandBiome("Frozen Crags",
    BiomeType.Desert,
    Subpolar <=> Polar,
    0.`mm/yr` <=> 250.`mm/yr`,
    Subalpine <=> SuperAlpine
  )

  val AlpineTundra = LandBiome("Alpine Tundra",
    BiomeType.GrasslandScrublandSavanna,
    Tropical <=> Polar,
    250.`mm/yr` <=> 750.`mm/yr`,
    Alpine <=> SuperAlpine
  )

  val ArcticTundra = LandBiome("Arctic Tundra",
    BiomeType.GrasslandScrublandSavanna,
    Subpolar <=> Subpolar,
    250.`mm/yr` <=> 750.`mm/yr`,
    Lowlands <=> Subalpine
  )

  val AntarcticTundra = LandBiome("Antarctic Tundra",
    BiomeType.GrasslandScrublandSavanna,
    Polar <=> Polar,
    250.`mm/yr` <=> 750.`mm/yr`,
    Lowlands <=> Subalpine
  )

  val Glacier = LandBiome("Glacier",
    BiomeType.Desert,
    Tropical <=> Polar,
    750.`mm/yr` <=> 10000.`mm/yr`,
    Subalpine <=> SuperAlpine
  )

  val BorealGlacier = LandBiome("Glacier",
    BiomeType.Desert,
    Boreal <=> Boreal,
    750.`mm/yr` <=> 10000.`mm/yr`,
    Midlands <=> SuperAlpine
  )

  val BarrenCliffs = LandBiome("Barren Cliffs",
    BiomeType.Desert,
    Tropical <=> Boreal,
    0.`mm/yr` <=> 25.`mm/yr`,
    Montane <=> SuperAlpine
  )

  //T: [-5, 5]
  val Taiga = LandBiome("Taiga",
    BiomeType.Forest,
    Boreal <=> Boreal,
    250.`mm/yr` <=> 750.`mm/yr`,
    Midlands <=> Subalpine
  )

  val BorealWetlands = LandBiome("Boreal Wetlands",
    BiomeType.Wetlands,
    Boreal <=> Boreal,
    250.`mm/yr` <=> 10000.`mm/yr`,
    Lowlands <=> Lowlands
  )

  val BorealGrassland = LandBiome("Boreal Grassland",
    BiomeType.GrasslandScrublandSavanna,
    Boreal <=> Boreal,
    25.`mm/yr` <=> 250.`mm/yr`,
    Lowlands <=> Montane
  )

  val BorealDesert = LandBiome("Boreal Desert",
    BiomeType.Desert,
    Boreal <=> Boreal,
    0.`mm/yr` <=> 25.`mm/yr`,
    Lowlands <=> Montane
  )

  val TropicalGrassland = LandBiome("Tropical Grassland",
    BiomeType.GrasslandScrublandSavanna,
    Tropical <=> Tropical,
    250.`mm/yr` <=> 500.`mm/yr`,
    Lowlands <=> Highlands
  )

  val TropicalSavanna = LandBiome("Tropical Savanna",
    BiomeType.GrasslandScrublandSavanna,
    Tropical <=> Subtropical,
    500.`mm/yr` <=> 1300.`mm/yr`,
    Lowlands <=> Highlands
  )

  val SubtropicalScrubland = LandBiome("Subtropical Scrubland",
    BiomeType.GrasslandScrublandSavanna,
    Subtropical <=> Subtropical,
    250.`mm/yr` <=> 500.`mm/yr`,
    Lowlands <=> Highlands
  )

  val PeatSwampForest = LandBiome("Peat Swamp Forest",
    BiomeType.Wetlands,
    Tropical <=> Subtropical,
    1300.`mm/yr` <=> 2000.`mm/yr`,
    Lowlands <=> Lowlands
  )

  val SubtropicalDeciduousForest = LandBiome("Subtropical Deciduous Forest",
    BiomeType.Forest,
    Tropical <=> Subtropical,
    1300.`mm/yr` <=> 2000.`mm/yr`,
    Midlands <=> Highlands
  )

  //http://earthobservatory.nasa.gov/Experiments/Biome/biorainforest.php
  val TropicalRainforest = LandBiome("Tropical Rainforest",
    BiomeType.Forest,
    Tropical <=> Subtropical,
    2000.`mm/yr` <=> 10000.`mm/yr`,
    Midlands <=> Highlands
  )

  val Mangroves = LandBiome("Mangroves",
    BiomeType.Wetlands,
    Tropical <=> Subtropical,
    2000.`mm/yr` <=> 10000.`mm/yr`,
    Lowlands <=> Lowlands
  )

  val MontaneRainforest = LandBiome("Montane Rainforest",
    BiomeType.Forest,
    Subtropical <=> CoolTemperate,
    2000.`mm/yr` <=> 10000.`mm/yr`,
    Montane <=> Subalpine
  )

  //http://earthobservatory.nasa.gov/Experiments/Biome/biorainforest.php
  val TemperateRainforest = LandBiome("Temperate Rainforest",
    BiomeType.Forest,
    WarmTemperate <=> CoolTemperate,
    2000.`mm/yr` <=> 10000.`mm/yr`,
    Midlands <=> Highlands
  )

  val SubtropicalBroadleafForest = LandBiome("Subtropical Broadleaf Forest",
    BiomeType.Forest,
    Subtropical <=> Subtropical,
    1500.`mm/yr` <=> 2000.`mm/yr`,
    Midlands <=> Montane
  )

  val TemperateBroadleafForest = LandBiome("Temperate Broadleaf Forest",
    BiomeType.Forest,
    WarmTemperate <=> CoolTemperate,
    1500.`mm/yr` <=> 2000.`mm/yr`,
    Midlands <=> Montane
  )

  //http://answers.yahoo.com/question/index?qid=20120201161323AAOwMtY
  val TemperateDeciduousForest = LandBiome("Temperate Deciduous Forest",
    BiomeType.Forest,
    WarmTemperate <=> CoolTemperate,
    750.`mm/yr` <=> 1500.`mm/yr`,
    Midlands <=> Montane
  )

  //http://earthobservatory.nasa.gov/Experiments/Biome/bioconiferous.php
  val TemperateConiferousForest = LandBiome("Temperate Coniferous Forest",
    BiomeType.Forest,
    WarmTemperate <=> CoolTemperate,
    300.`mm/yr` <=> 750.`mm/yr`,
    Midlands <=> Subalpine
  )

  val TemperateWetlands = LandBiome("Temperate Wetlands",
    BiomeType.Wetlands,
    WarmTemperate <=> CoolTemperate,
    180.`mm/yr` <=> 10000.`mm/yr`,
    Lowlands <=> Lowlands
  )

  val Steppe = LandBiome("Steppe",
    BiomeType.GrasslandScrublandSavanna,
    Subtropical <=> CoolTemperate,
    250.`mm/yr` <=> 750.`mm/yr`,
    Midlands <=> Highlands
  )

  val TemperateScrubland = LandBiome("Temperate Scrubland",
    BiomeType.GrasslandScrublandSavanna,
    WarmTemperate <=> CoolTemperate,
    25.`mm/yr` <=> 180.`mm/yr`,
    Lowlands <=> Lowlands
  )

  //approximately 1 in. (.25 cm) of rain falls in dry deserts per year.
  //The latitude range is 15-28° north and south of the equator.
  //http://www.blueplanetbiomes.org/desert_climate_page.htm
  val AridDesert = LandBiome("Arid Desert",
    BiomeType.Desert,
    Tropical <=> CoolTemperate,
    0.`mm/yr` <=> 25.`mm/yr`,
    Lowlands <=> Highlands
  )

  val XericScrubland = LandBiome("Xeric Scrubland",
    BiomeType.GrasslandScrublandSavanna,
    Tropical <=> Subtropical,
    25.`mm/yr` <=> 250.`mm/yr`,
    Lowlands <=> Highlands
  )

  val TallGrassland = LandBiome("Tall Temperate Grassland",
    BiomeType.GrasslandScrublandSavanna,
    WarmTemperate <=> CoolTemperate,
    250.`mm/yr` <=> 300.`mm/yr`,
    Lowlands <=> Highlands
  )

  val MontaneGrassland = LandBiome("Montane Grassland",
    BiomeType.GrasslandScrublandSavanna,
    Subtropical <=> Boreal,
    25.`mm/yr` <=> 300.`mm/yr`,
    Montane <=> Alpine
  )

  val ShortGrassland = LandBiome("Short Temperate Grassland",
    BiomeType.GrasslandScrublandSavanna,
    WarmTemperate <=> CoolTemperate,
    25.`mm/yr` <=> 250.`mm/yr`,
    Lowlands <=> Highlands
  )

  val Hellscape = LandBiome("Hellscape",
    BiomeType.Desert,
    SuperTropical <=> SuperTropical,
    0.`mm/yr` <=> 0.`mm/yr`,
    Lowlands <=> Highlands
  )

  val Infestation = LandBiome("Infestation",
    BiomeType.Wetlands,
    SuperTropical <=> SuperTropical,
    0.01.`mm/yr` <=> 2000.`mm/yr`,
    Lowlands <=> Highlands
  )

  val Maelstrom = LandBiome("Maelstrom",
    BiomeType.Special,
    SuperTropical <=> SuperTropical,
    2000.`mm/yr` <=> 7000.`mm/yr`,
    Lowlands <=> Highlands
  )

  val Roil = LandBiome("Infestation",
    BiomeType.Special,
    SuperTropical <=> SuperTropical,
    7000.`mm/yr` <=> 10000.`mm/yr`,
    Lowlands <=> Highlands
  )

   val All:Vector[Biome] = Reflection.getEnum(Biomes, this.getClass, "Biome", _ == "Void")


 }
