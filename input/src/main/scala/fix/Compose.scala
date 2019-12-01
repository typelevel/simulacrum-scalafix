/*
rules = [
  TypeClassSupport
]
 */
package cats
package arrow

import simulacrum.{op, typeclass}

/**
 * Must obey the laws defined in cats.laws.ComposeLaws.
 *
 * Here's how you can use `>>>` and `<<<`
 * Example:
 * {{{
 * scala> import cats.implicits._
 * scala> val f : Int => Int = (_ + 1)
 * scala> val g : Int => Int = (_ * 100)
 * scala> (f >>> g)(3)
 * res0: Int = 400
 * scala> (f <<< g)(3)
 * res1: Int = 301
 * }}}
 */
@typeclass trait Compose[F[_, _]] { self =>
  @simulacrum.op("<<<", alias = true)
  def compose[A, B, C](f: F[B, C], g: F[A, B]): F[A, C]

  @op(">>>", alias = false)
  def andThen[A, B, C](f: F[A, B], g: F[B, C]): F[A, C] =
    compose(g, f)
}

object Compose

@typeclass trait Category[F[_, _]] extends Compose[F] { self =>
  def id[A]: F[A, A]
}

object Category {}

/**
 * A [[Profunctor]] is a [[Contravariant]] functor on its first type parameter
 * and a [[Functor]] on its second type parameter.
 *
 * Must obey the laws defined in cats.laws.ProfunctorLaws.
 */
@typeclass trait Profunctor[F[_, _]] { self =>

  /**
   * Contramap on the first type parameter and map on the second type parameter
   *
   * Example:
   * {{{
   * scala> import cats.implicits._
   * scala> import cats.arrow.Profunctor
   * scala> val fab: Double => Double = x => x + 0.3
   * scala> val f: Int => Double = x => x.toDouble / 2
   * scala> val g: Double => Double = x => x * 3
   * scala> val h = Profunctor[Function1].dimap(fab)(f)(g)
   * scala> h(3)
   * res0: Double = 5.4
   * }}}
   */
  def dimap[A, B, C, D](fab: F[A, B])(f: C => A)(g: B => D): F[C, D]

  /**
   * contramap on the first type parameter
   */
  final def lmap[A, B, C](fab: F[A, B])(f: C => A): F[C, B] =
    dimap(fab)(f)(identity)

  /**
   * map on the second type parameter
   */
  @inline final def rmap[A, B, C](fab: F[A, B])(f: B => C): F[A, C] =
    dimap[A, B, A, C](fab)(identity)(f)
}
