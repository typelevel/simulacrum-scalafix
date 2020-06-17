/*
rules = [
  AddImplicitNotFound,
  TypeClassSupport,
  AddSerializable
]
 */
package cats

import scala.annotation.implicitNotFound
import simulacrum.typeclass

/**
 * Must obey the laws defined in cats.laws.InvariantLaws.
 */
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

object Invariant {}

@typeclass
trait InvariantSemigroupal[F[_]] extends Invariant[F] { self =>
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]
  override def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B]
}

@typeclass
trait ContravariantSemigroupal[F[_]] { self => }

object ContravariantSemigroupal {

  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */
  val oldStuffThatShouldBeReplaced = ()

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */
  val thisShouldBePreserved = ()
}
