package com.deweyvm.dogue.common.procgen

import com.deweyvm.gleany.data.Point2d
import com.deweyvm.dogue.common.Implicits
import Implicits._

object Polygon {
  def fromPoints(points:Vector[Point2d]):Polygon = {
    val toPair = points(points.length - 1) +: points
    val lines = (0 until toPair.length - 1).map { i =>
      val p1 = toPair(i)
      val p2 = toPair(i+1)
      Line(p1, p2)
    }
    Polygon(lines.toSet)
  }


  def test() {
    val poly = Polygon(Set(
      new Line(1,1,3,2),
      new Line(3,2,4,1),
      new Line(4,1,6,4),
      new Line(6,4,7,1),
      new Line(7,1,7,7),
      new Line(7,7,4,3),
      new Line(4,3,5,6),
      new Line(5,6,3,4),
      new Line(3,4,2,7),
      new Line(2,7,1,6),
      new Line(1,6,1,1)


    ))
    val points = List(
      (Point2d(0,0), false),
      (Point2d(2,3), true),
      (Point2d(4,4), true),
      (Point2d(5,4), true),
      (Point2d(6,2), false),
      (Point2d(5,5), false),
      (Point2d(6,5), true),
      (Point2d(3,6), false),
      (Point2d(2,6), true)

    )

    points foreach { case (p, expect) =>
      val v = poly.contains(p)
      assert (v == expect, "%s expected (%s) got (%s)" format (p, expect, v))
    }

  }

}

case class Polygon(lines:Set[Line]) {

  def isAdjacent(other:Polygon):Boolean = {
    lines exists other.lines.contains
  }

  def contains(pt:Point2d):Boolean = {
    val ray = new Line(pt, Point2d(Int.MaxValue, 5000))
    val intersections = lines.foldLeft(0){case (acc, line) =>
      if (ray.intersectPoint(line).isDefined) {
        acc + 1
      } else {
        acc
      }
    }
    intersections.isOdd

  }

  def scale(s:Double) = {
    copy(lines = lines map {_.scale(s)})
  }

  override def equals(obj:Any) = {
    if (!obj.isInstanceOf[Polygon]) {
      false
    } else {
      val other = obj.asInstanceOf[Polygon]
      other.lines == lines
    }
  }

  override def hashCode() = lines.hashCode()
}
