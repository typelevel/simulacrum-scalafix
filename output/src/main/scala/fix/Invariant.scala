package cats

import scala.annotation.implicitNotFound
import simulacrum.typeclass

/**
 * Must obey the laws defined in cats.laws.InvariantLaws.
 */
@implicitNotFound("Could not find an instance of Invariant for ${F}")
@typeclass
trait Invariant[F[_]] extends Serializable { self =>

  /**
   * Transform an `F[A]` into an `F[B]` by providing a transformation from `A`
   * to `B` and one from `B` to `A`.
   *
   * Example:
   * {{{
   * scala> import cats.implicits._
   * scala> import scala.concurrent.duration._
   * scala> val durSemigroup: Semigroup[FiniteDuration] =
   *      | Invariant[Semigroup].imap(Semigroup[Long])(Duration.fromNanos)(_.toNanos)
   * scala> durSemigroup.combine(2.seconds, 3.seconds)
   * res1: FiniteDuration = 5 seconds
   * }}}
   */
  def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B]
}

object Invariant {
  /****************************************************************************
   * THE REST OF THIS OBJECT IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!! *
   ****************************************************************************/

  /**
   * Summon an instance of [[Invariant]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: Invariant[F]): Invariant[F] = instance

  trait Ops[F[_], A] {
    type TypeClassType <: Invariant[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
    def imap[B](f: A => B)(g: B => A): F[B] = typeClassInstance.imap[A, B](self)(f)(g)
  }
  trait AllOps[F[_], A] extends Ops[F, A]
  trait ToInvariantOps {
    implicit def toInvariantOps[F[_], A](target: F[A])(implicit tc: Invariant[F]): Ops[F, A] {
      type TypeClassType = Invariant[F]
    } = new Ops[F, A] {
      type TypeClassType = Invariant[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToInvariantOps
  object ops {
    implicit def toAllInvariantOps[F[_], A](target: F[A])(implicit tc: Invariant[F]): AllOps[F, A] {
      type TypeClassType = Invariant[F]
    } = new AllOps[F, A] {
      type TypeClassType = Invariant[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
}

@implicitNotFound("Could not find an instance of InvariantSemigroupal for ${F}")
@typeclass
trait InvariantSemigroupal[F[_]] extends Invariant[F] { self =>
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]
  override def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B]
}

object InvariantSemigroupal {
  /****************************************************************************
   * THE REST OF THIS OBJECT IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!! *
   ****************************************************************************/

  /**
   * Summon an instance of [[InvariantSemigroupal]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: InvariantSemigroupal[F]): InvariantSemigroupal[F] = instance

  trait Ops[F[_], A] {
    type TypeClassType <: InvariantSemigroupal[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
    def product[B](fb: F[B]): F[(A, B)] = typeClassInstance.product[A, B](self, fb)
  }
  trait AllOps[F[_], A] extends Ops[F, A] with Invariant.AllOps[F, A] {
    type TypeClassType <: InvariantSemigroupal[F]
  }
  trait ToInvariantSemigroupalOps {
    implicit def toInvariantSemigroupalOps[F[_], A](target: F[A])(implicit tc: InvariantSemigroupal[F]): Ops[F, A] {
      type TypeClassType = InvariantSemigroupal[F]
    } = new Ops[F, A] {
      type TypeClassType = InvariantSemigroupal[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToInvariantSemigroupalOps
  object ops {
    implicit def toAllInvariantSemigroupalOps[F[_], A](target: F[A])(implicit tc: InvariantSemigroupal[F]): AllOps[F, A] {
      type TypeClassType = InvariantSemigroupal[F]
    } = new AllOps[F, A] {
      type TypeClassType = InvariantSemigroupal[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
}

@implicitNotFound("Could not find an instance of ContravariantSemigroupal for ${F}")
@typeclass
trait ContravariantSemigroupal[F[_]] extends Serializable { self =>
}

object ContravariantSemigroupal {
  /****************************************************************************
   * THE REST OF THIS OBJECT IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!! *
   ****************************************************************************/

  /**
   * Summon an instance of [[ContravariantSemigroupal]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: ContravariantSemigroupal[F]): ContravariantSemigroupal[F] = instance

  trait Ops[F[_], A] {
    type TypeClassType <: ContravariantSemigroupal[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
  }
  trait AllOps[F[_], A] extends Ops[F, A]
  trait ToContravariantSemigroupalOps {
    implicit def toContravariantSemigroupalOps[F[_], A](target: F[A])(implicit tc: ContravariantSemigroupal[F]): Ops[F, A] {
      type TypeClassType = ContravariantSemigroupal[F]
    } = new Ops[F, A] {
      type TypeClassType = ContravariantSemigroupal[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToContravariantSemigroupalOps
  object ops {
    implicit def toAllContravariantSemigroupalOps[F[_], A](target: F[A])(implicit tc: ContravariantSemigroupal[F]): AllOps[F, A] {
      type TypeClassType = ContravariantSemigroupal[F]
    } = new AllOps[F, A] {
      type TypeClassType = ContravariantSemigroupal[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
}
