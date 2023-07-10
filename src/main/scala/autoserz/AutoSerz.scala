package com.radix.shared.persistence.autoserz

import scala.annotation.StaticAnnotation

case class AutoSerz(params: AutoSerzParams*) extends StaticAnnotation

sealed trait AutoSerzParams

/**
 * Includes an implicit ExtendedActorSystem in the generated serializer, which is necessary for serializing classes containing
 * ActorRefs and StreamRefs.
 */
case object ExtendedActorSystem extends AutoSerzParams

/**
 * Generates cachedImplicit SchemaFor/Encoder/Decoders for the generated serializer. This can produce a significant
 * performance improvement when used on classes that are included in other serialized classes, as otherwise the former
 * class will have to be re-serialized each time.
 */
case object CachedImplicits extends AutoSerzParams
object AutoSerzParams {
  def parse(str: String): AutoSerzParams = {
    str match {
      case "ExtendedActorSystem" => ExtendedActorSystem
      case "CachedImplicits"     => CachedImplicits
      case _                     => throw new Exception(s"Unknown AutoSerzParam: $str")
    }
  }
}
