package org.geoscript.example

import math._

import java.io.File
import java.io.Serializable

import org.geotools.data.FeatureSource
import org.geotools.factory.{ CommonFactoryFinder, GeoTools }
import org.geotools.feature.FeatureCollection
import org.geotools.styling.{ SLDTransformer, Style }
import org.geotools.util.SimpleInternationalString

import org.geoscript._

object ColorRamp extends org.geoscript.feature.GeoCrunch {
  def pairwise[A](s: List[A]): List[(A,A)] = s zip (s drop 1)

  def ranges(col: feature.FeatureCollection, p: String): List[(Double, Double)] =
  {
    // Use the value for the first feature as the starting value 
    // for both max and min

    var upper = Double.NegativeInfinity
    var lower = Double.PositiveInfinity

    for (f <- col) { 
      val current = f.get[Double](p)
      lower = min(lower, current)
      upper = max(upper, current)
    }

    // find a position on the ramp by taking a weighted 
    // average of the max and min values.
    def ramp(weight: Double) = lower + (upper - lower) * weight

    // create 10 pairs representing ranges between min and max, linearly spaced
    pairwise((0 to 10).toList.map(x => ramp(x / 10d)))
  }

  def colorRamp(data: layer.Layer, property: String): Style = {
    val styles = CommonFactoryFinder.getStyleFactory(null)
    val filters = CommonFactoryFinder.getFilterFactory(null)

    def rule(x: ((Double, Double), Int)): org.geotools.styling.Rule = {
      val ((min, max), index) = x

      val rule = styles.createRule
      rule.setFilter(filters.between(
        filters.property(property),
        filters.literal(min),
        filters.literal(max)
      ))
      val color = java.awt.Color.getHSBColor(index/10.0f, 0.5f, 0.75f)
      val colorExpr = filters.literal("#%2x%2x%2x".format(
        color.getRed,
        color.getGreen,
        color.getBlue)
      )
      rule.symbolizers.add(styles.createPolygonSymbolizer(
        styles.getDefaultStroke,
        styles.createFill(colorExpr),
        null
      ))
      rule.setName("%s#%d".format(property, index))
      rule.setTitle("%s in [%.1f <-> %.1f]".format(property, min, max))
      rule.getDescription().setAbstract(new SimpleInternationalString(
        "Values of %s between %.1f and %.1f".format(
          property, min, max
        )
      ))
      return rule
    }

    val style = styles.createStyle
    style.setName(data.schema.name)
    style.getDescription().setTitle(new SimpleInternationalString(
      "Color Ramp for %s".format(data.schema.name)
    ))
    style.getDescription().setAbstract(new SimpleInternationalString(
      "Autogenerated color ramp for %s based on %s".format(
        data.schema.name, property
      )
    ))

    val ramp = ranges(data.features, property)
    val ftStyle = 
      styles.createFeatureTypeStyle(ramp.zipWithIndex.map(rule).toArray)
    style.featureTypeStyles.add(ftStyle)

    return style
  }

  def main(args: Array[String]) = {
    val file = promptShapeFile.getAbsolutePath
    val shp = layer.Shapefile(file)

    val xformer = new SLDTransformer
    val sld = promptSaveFile(new javax.swing.filechooser.FileFilter() {
        def accept(f: File): Boolean = {
          f.isDirectory || 
          f.getPath.toLowerCase.endsWith("sld") ||
          f.getPath.toLowerCase.endsWith("xml")
        }

        def getDescription(): String = "SLD (Styled Layer Descriptor)"
    })

    val sldStream = new java.io.FileOutputStream(sld)
    xformer.setIndentation(2)
    xformer.transform(colorRamp(shp, "PERSONS"), sldStream)
    sldStream.flush()
    sldStream.close()
  }
}
