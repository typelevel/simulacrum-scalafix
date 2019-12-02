package org.typelevel.simulacrum.fix

sealed trait Variance

object Variance {
  case object Invariant extends Variance
  case object Covariant extends Variance
  case object Contravariant extends Variance
}
