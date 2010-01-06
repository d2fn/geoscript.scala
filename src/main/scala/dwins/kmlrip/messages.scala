package dwins.kmlrip

import java.net.URL

import scala.actors.Actor

case class Ready(a: Actor)
case class Done
case class ReadUrl(url: URL)
case class Document(url: URL, valid: Boolean, mime:String, length:Int)
case class Feature(source: URL, id: String)
case class Link(source: URL, target: URL)