package cats

import simulacrum.typeclass

/**
 * Some type class.
 */
@typeclass(List("bad"), generateAllOps = false)
trait MissingCompanionAndSummoner[F[_]]

object MissingCompanionAndSummoner {
  /****************************************************************************
   * THE REST OF THIS OBJECT IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!! *
   ****************************************************************************/

  /**
   * Summon an instance of [[MissingCompanionAndSummoner]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: MissingCompanionAndSummoner[F]): MissingCompanionAndSummoner[F] = instance

  trait Ops[F[_], A] {
    type TypeClassType <: MissingCompanionAndSummoner[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
  }
  trait AllOps[F[_], A] extends Ops[F, A]
  trait ToMissingCompanionAndSummonerOps {
    implicit def toMissingCompanionAndSummonerOps[F[_], A](target: F[A])(implicit tc: MissingCompanionAndSummoner[F]): Ops[F, A] {
      type TypeClassType = MissingCompanionAndSummoner[F]
    } = new Ops[F, A] {
      type TypeClassType = MissingCompanionAndSummoner[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToMissingCompanionAndSummonerOps
  object ops {
    implicit def toAllMissingCompanionAndSummonerOps[F[_], A](target: F[A])(implicit tc: MissingCompanionAndSummoner[F]): AllOps[F, A] {
      type TypeClassType = MissingCompanionAndSummoner[F]
    } = new AllOps[F, A] {
      type TypeClassType = MissingCompanionAndSummoner[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
}

/**
 * Some type class.
 */
@typeclass trait MissingSummoner[F[_]] extends Serializable

object MissingSummoner {
  def other: String = ""

  /****************************************************************************
   * THE REST OF THIS OBJECT IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!! *
   ****************************************************************************/

  /**
   * Summon an instance of [[MissingSummoner]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: MissingSummoner[F]): MissingSummoner[F] = instance

  trait Ops[F[_], A] {
    type TypeClassType <: MissingSummoner[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
  }
  trait AllOps[F[_], A] extends Ops[F, A]
  trait ToMissingSummonerOps {
    implicit def toMissingSummonerOps[F[_], A](target: F[A])(implicit tc: MissingSummoner[F]): Ops[F, A] {
      type TypeClassType = MissingSummoner[F]
    } = new Ops[F, A] {
      type TypeClassType = MissingSummoner[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToMissingSummonerOps
  object ops {
    implicit def toAllMissingSummonerOps[F[_], A](target: F[A])(implicit tc: MissingSummoner[F]): AllOps[F, A] {
      type TypeClassType = MissingSummoner[F]
    } = new AllOps[F, A] {
      type TypeClassType = MissingSummoner[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
}
