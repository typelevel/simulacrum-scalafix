package simulacrum

import scala.annotation.Annotation

final class typeclass(excludeParents: List[String] = Nil, generateAllOps: Boolean = true) extends Annotation
final class op(name: String, alias: Boolean = false) extends Annotation
final class noop() extends Annotation
