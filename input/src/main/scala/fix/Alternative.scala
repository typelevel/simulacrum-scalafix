/*
rules = [
  TypeClassSupport
]
 */
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

object Alternative extends AlternativeFunctions

trait AlternativeFunctions

trait Monad[F[_]]
trait Bifoldable[F[_]]
