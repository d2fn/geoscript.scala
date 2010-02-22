package org.geoscript.geometry

import com.vividsolutions.jts.{geom=>jts}
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory
import org.opengis.referencing.crs.CoordinateReferenceSystem
import org.geoscript.projection.Projection

/**
 * A companion object for the LineString type, providing various
 * methods for directly instantiating LineString objects.
 */
object LineString {
  private val preparingFactory = new PreparedGeometryFactory()

  private class Wrapper(val underlying: jts.LineString) extends LineString {
    override def prepare() = 
    if (prepared) {
      this
    } else {
      val prep =
        preparingFactory.create(underlying).asInstanceOf[jts.LineString]
      new Wrapper(prep) {
        override def prepared = true
      }
    }

    def in(dest: Projection) = new Projected(underlying, dest)
  }

  private class Projected(
    val underlying: jts.LineString,
    override val projection: Projection
  ) extends LineString {
    
    override def prepare() = 
      if (prepared) {
        this
      } else {
        val prep =
          preparingFactory.create(underlying).asInstanceOf[jts.LineString]
        new Projected(prep, projection) {
          override def prepared = true
        }
      }

    def in(dest: Projection) = 
      new Projected(projection.to(dest)(underlying), dest)
  }

  /**
   * Create a LineString by wrapping a "raw" JTS LineString.
   */
  def apply(line: jts.LineString): LineString = new Wrapper(line)

  /**
   * Create a LineString from JTS Coordinates.
   */
  def apply(coords: jts.Coordinate*): LineString =
    new Wrapper(
      ModuleInternals.factory.createLineString(coords toArray)
    )
}

/**
 * A LineString contains 0 or more contiguous line segments, and is useful for
 * representing geometries such as roads or rivers.
 */
trait LineString extends Geometry {
  override val underlying: jts.LineString
  override def in(dest: Projection): LineString
  override def transform(dest: Projection): LineString = 
    LineString(projection.to(dest)(underlying)) in dest
}
