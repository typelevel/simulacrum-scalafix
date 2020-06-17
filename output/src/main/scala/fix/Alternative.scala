package cats

import simulacrum.typeclass

@typeclass trait Alternative[F[_]] { self =>

  /**
   * Fold over the inner structure to combine all of the values with
   * our combine method inherited from MonoidK. The result is for us
   * to accumulate all of the "interesting" values of the inner G, so
   * if G is Option, we collect all the Some values, if G is Either,
   * we collect all the Right values, etc.
   *
   * Example:
   * {{{
   * scala> import cats.implicits._
   * scala> val x: List[Vector[Int]] = List(Vector(1, 2), Vector(3, 4))
   * scala> Alternative[List].unite(x)
   * res0: List[Int] = List(1, 2, 3, 4)
   * }}}
   */
  def unite[G[_], A](fga: F[G[A]])(implicit FM: Monad[F], G: Bifoldable[G]): F[A]

  /**
   * Return ().pure[F] if `condition` is true, `empty` otherwise
   *
   * Example:
   * {{{
   * scala> import cats.implicits._
   * scala> def even(i: Int): Option[String] = Alternative[Option].guard(i % 2 == 0).as("even")
   * scala> even(2)
   * res0: Option[String] = Some(even)
   * scala> even(3)
   * res1: Option[String] = None
   * }}}
   */
  def guard(condition: Boolean): F[Unit]
}

object Alternative extends AlternativeFunctions {
  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[Alternative]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: Alternative[F]): Alternative[F] = instance

  trait Ops[F[_], A] extends Serializable {
    type TypeClassType <: Alternative[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
    def unite[G[_], B](implicit ev$1: A <:< G[B], FM: Monad[F], G: Bifoldable[G]): F[B] = typeClassInstance.unite[G, B](self.asInstanceOf[F[G[B]]])(FM, G)
  }
  trait AllOps[F[_], A] extends Ops[F, A]
  trait ToAlternativeOps extends Serializable {
    implicit def toAlternativeOps[F[_], A](target: F[A])(implicit tc: Alternative[F]): Ops[F, A] {
      type TypeClassType = Alternative[F]
    } = new Ops[F, A] {
      type TypeClassType = Alternative[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  @deprecated("Use cats.syntax object imports", "2.2.0")
  object nonInheritedOps extends ToAlternativeOps
  @deprecated("Use cats.syntax object imports", "2.2.0")
  object ops {
    implicit def toAllAlternativeOps[F[_], A](target: F[A])(implicit tc: Alternative[F]): AllOps[F, A] {
      type TypeClassType = Alternative[F]
    } = new AllOps[F, A] {
      type TypeClassType = Alternative[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}

trait AlternativeFunctions

trait Monad[F[_]]
trait Bifoldable[F[_]]
