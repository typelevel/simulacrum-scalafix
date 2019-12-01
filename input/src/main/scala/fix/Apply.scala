/*
rules = [
  AddImplicitNotFound,
  TypeClassSupport,
  AddSerializable
]
 */
package cats

@simulacrum.typeclass(excludeParents = List("ApplyArityFunctions"))
trait Apply[F[_]] extends InvariantSemigroupal[F] with ApplyArityFunctions[F] { self =>

  /**
   * Given a value and a function in the Apply context, applies the
   * function to the value.
   *
   * Example:
   * {{{
   * scala> import cats.implicits._
   *
   * scala> val someF: Option[Int => Long] = Some(_.toLong + 1L)
   * scala> val noneF: Option[Int => Long] = None
   * scala> val someInt: Option[Int] = Some(3)
   * scala> val noneInt: Option[Int] = None
   *
   * scala> Apply[Option].ap(someF)(someInt)
   * res0: Option[Long] = Some(4)
   *
   * scala> Apply[Option].ap(noneF)(someInt)
   * res1: Option[Long] = None
   *
   * scala> Apply[Option].ap(someF)(noneInt)
   * res2: Option[Long] = None
   *
   * scala> Apply[Option].ap(noneF)(noneInt)
   * res3: Option[Long] = None
   * }}}
   */
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]

  def map[A, B](fa: F[A])(f: A => B): F[B]

  def productR[A, B](fa: F[A])(fb: F[B]): F[B] =
    ap(map(fa)(_ => (b: B) => b))(fb)
}

@simulacrum.typeclass
trait FlatMap[F[_]] extends Apply[F] {
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  /**
   * "flatten" a nested `F` of `F` structure into a single-layer `F` structure.
   *
   * This is also commonly called `join`.
   *
   * Example:
   * {{{
   * scala> import cats.Eval
   * scala> import cats.implicits._
   *
   * scala> val nested: Eval[Eval[Int]] = Eval.now(Eval.now(3))
   * scala> val flattened: Eval[Int] = nested.flatten
   * scala> flattened.value
   * res0: Int = 3
   * }}}
   */
  def flatten[A](ffa: F[F[A]]): F[A] =
    flatMap(ffa)(fa => fa)
}

trait ApplyArityFunctions[F[_]] {
  def bad[A, B](f: F[A]): B
}
