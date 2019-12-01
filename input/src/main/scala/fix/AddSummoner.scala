/*
rule = TypeClassSupport
 */
package cats

import simulacrum.typeclass

/**
 * Some type class.
 */
@typeclass(List("bad"), generateAllOps = false)
trait MissingCompanionAndSummoner[F[_]]

/**
 * Some type class.
 */
@typeclass trait MissingSummoner[F[_]] extends Serializable

object MissingSummoner {
  def other: String = ""
}
