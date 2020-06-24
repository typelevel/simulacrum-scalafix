package simulacrum

import scala.annotation.{Annotation, compileTimeOnly}

@compileTimeOnly("typeclass annotation should not be used after Scalafix rule application")
final class typeclass(excludeParents: List[String] = Nil, generateAllOps: Boolean = true) extends Annotation

@compileTimeOnly("op annotation should not be used after Scalafix rule application")
final class op(name: String, alias: Boolean = false) extends Annotation

@compileTimeOnly("noop annotation should not be used after Scalafix rule application")
final class noop() extends Annotation
