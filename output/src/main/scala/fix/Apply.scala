package cats

import scala.annotation.implicitNotFound
@implicitNotFound("Could not find an instance of Apply for ${F}")
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

object Apply {
  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[Apply]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: Apply[F]): Apply[F] = instance

  object ops {
    implicit def toAllApplyOps[F[_], A](target: F[A])(implicit tc: Apply[F]): AllOps[F, A] {
      type TypeClassType = Apply[F]
    } = new AllOps[F, A] {
      type TypeClassType = Apply[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[F[_], A] extends Serializable {
    type TypeClassType <: Apply[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
    def ap[B, C](fa: F[B])(implicit ev$1: A <:< (B => C)): F[C] = typeClassInstance.ap[B, C](self.asInstanceOf[F[B => C]])(fa)
    def map[B](f: A => B): F[B] = typeClassInstance.map[A, B](self)(f)
    def productR[B](fb: F[B]): F[B] = typeClassInstance.productR[A, B](self)(fb)
  }
  trait AllOps[F[_], A] extends Ops[F, A] with InvariantSemigroupal.AllOps[F, A] {
    type TypeClassType <: Apply[F]
  }
  trait ToApplyOps extends Serializable {
    implicit def toApplyOps[F[_], A](target: F[A])(implicit tc: Apply[F]): Ops[F, A] {
      type TypeClassType = Apply[F]
    } = new Ops[F, A] {
      type TypeClassType = Apply[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToApplyOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}

@implicitNotFound("Could not find an instance of FlatMap for ${F}")
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

object FlatMap {
  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[FlatMap]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: FlatMap[F]): FlatMap[F] = instance

  object ops {
    implicit def toAllFlatMapOps[F[_], A](target: F[A])(implicit tc: FlatMap[F]): AllOps[F, A] {
      type TypeClassType = FlatMap[F]
    } = new AllOps[F, A] {
      type TypeClassType = FlatMap[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[F[_], A] extends Serializable {
    type TypeClassType <: FlatMap[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
    def flatMap[B](f: A => F[B]): F[B] = typeClassInstance.flatMap[A, B](self)(f)
    def flatten[B](implicit ev$1: A <:< F[B]): F[B] = typeClassInstance.flatten[B](self.asInstanceOf[F[F[B]]])
  }
  trait AllOps[F[_], A] extends Ops[F, A] with Apply.AllOps[F, A] {
    type TypeClassType <: FlatMap[F]
  }
  trait ToFlatMapOps extends Serializable {
    implicit def toFlatMapOps[F[_], A](target: F[A])(implicit tc: FlatMap[F]): Ops[F, A] {
      type TypeClassType = FlatMap[F]
    } = new Ops[F, A] {
      type TypeClassType = FlatMap[F]
      val self: F[A] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToFlatMapOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}

trait ApplyArityFunctions[F[_]] {
  def bad[A, B](f: F[A]): B
}
