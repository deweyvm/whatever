package com.deweyvm.dogue.world

import com.deweyvm.dogue.common.data.Array2d
import scala.collection.mutable.ArrayBuffer
import com.deweyvm.dogue.common.CommonImplicits
import CommonImplicits._
import com.deweyvm.dogue.common.procgen.PerlinParams

trait SurfaceType {
  val isWater:Boolean
}

object Surface {
  case object Land extends SurfaceType {
    val isWater = false
  }
  case object Water extends SurfaceType {
    val isWater = true
  }
}

class SurfaceMap(noise:Array2d[Double], params:PerlinParams) {
  private def perlinToHeight(t:Double) = {
    if (t > 0) {
      val tm = 1 - t
      1 - math.pow(tm, 0.1)
    } else {
      math.pow(t, 3)
    }
  }

  private val mountainSet = new TopoFeature(TopoFeature.mountain3, 50, noise)

  private val numDepressions = 10
  private val lakeSet = TopoFeature.create(TopoFeature.lake, numDepressions, params.copy(period=64))


  private val (lakes, basins) = lakeSet.extracted.splitAt(numDepressions/2)

  private val lakeHeight:Array2d[Double] = noise.transform {case (i, j, d) =>
    if (lakes.exists(_.contains((i, j)))) {
      lakeSet.get(i, j)
    } else {
      0
    }
  }

  private val basinHeight:Array2d[Double] = noise.transform {case (i, j, d) =>
    if (basins.exists(_.contains((i, j)))) {
      lakeSet.get(i, j)
    } else {
      0
    }
  }

  private def applyMountain(land:Double, mountain:Double) = {
    (mountain > 0) select ((land + mountain)/2, land)
  }

  val heightMap: Array2d[Meters] = noise.transform({ case (i, j, p) =>
    val base = perlinToHeight(p)
    val m = applyMountain(base, mountainSet.get(i, j))
    val h = m - lakeHeight.get(i, j) - basinHeight.get(i, j)
    h * 10000 m
  })



  private def isOcean(m:Meters):Boolean = m <= 0.0.m
  private def isLake(i:Int, j:Int):Boolean = lakes.exists {_.contains((i, j))}
  private val oceanPoints = {
    val buff = ArrayBuffer[(Int,Int)]()
    heightMap foreach { case (i, j, h) =>
      if (isOcean(h) || isLake(i, j)) {
        (buff += ((i, j))).ignore()
      }
    }
    buff.toSet
  }

  private val flooded = FloodFill.extract(None, heightMap, oceanPoints, isOcean)

  val landMap:Array2d[SurfaceType] = Array2d.tabulate(heightMap.rows, heightMap.cols) { case (i, j) =>
    if (flooded.exists{_.contains((i, j))}) {
      Surface.Water
    } else {
      Surface.Land
    }
  }
}
