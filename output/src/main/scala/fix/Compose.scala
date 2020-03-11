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

object Compose {
  /****************************************************************************
   * THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      *
   ****************************************************************************/

  /**
   * Summon an instance of [[Compose]] for `F`.
   */
  @inline def apply[F[_, _]](implicit instance: Compose[F]): Compose[F] = instance

  trait Ops[F[_, _], A, B] {
    type TypeClassType <: Compose[F]
    def self: F[A, B]
    val typeClassInstance: TypeClassType
    def compose[C](g: F[C, A]): F[C, B] = typeClassInstance.compose[C, A, B](self, g)
    def <<<[C](g: F[C, A]): F[C, B] = typeClassInstance.compose[C, A, B](self, g)
    def >>>[C](g: F[B, C]): F[A, C] = typeClassInstance.andThen[A, B, C](self, g)
  }
  trait AllOps[F[_, _], A, B] extends Ops[F, A, B]
  trait ToComposeOps {
    implicit def toComposeOps[F[_, _], A, B](target: F[A, B])(implicit tc: Compose[F]): Ops[F, A, B] {
      type TypeClassType = Compose[F]
    } = new Ops[F, A, B] {
      type TypeClassType = Compose[F]
      val self: F[A, B] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToComposeOps
  object ops {
    implicit def toAllComposeOps[F[_, _], A, B](target: F[A, B])(implicit tc: Compose[F]): AllOps[F, A, B] {
      type TypeClassType = Compose[F]
    } = new AllOps[F, A, B] {
      type TypeClassType = Compose[F]
      val self: F[A, B] = target
      val typeClassInstance: TypeClassType = tc
    }
  }

  /****************************************************************************
   * END OF SIMULACRUM-MANAGED CODE                                           *
   ****************************************************************************/

}

@typeclass trait Category[F[_, _]] extends Compose[F] { self =>
  def id[A]: F[A, A]
}

object Category {
  /****************************************************************************
   * THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      *
   ****************************************************************************/

  /**
   * Summon an instance of [[Category]] for `F`.
   */
  @inline def apply[F[_, _]](implicit instance: Category[F]): Category[F] = instance

  trait Ops[F[_, _], A, B] {
    type TypeClassType <: Category[F]
    def self: F[A, B]
    val typeClassInstance: TypeClassType
  }
  trait AllOps[F[_, _], A, B] extends Ops[F, A, B] with Compose.AllOps[F, A, B] {
    type TypeClassType <: Category[F]
  }
  trait ToCategoryOps {
    implicit def toCategoryOps[F[_, _], A, B](target: F[A, B])(implicit tc: Category[F]): Ops[F, A, B] {
      type TypeClassType = Category[F]
    } = new Ops[F, A, B] {
      type TypeClassType = Category[F]
      val self: F[A, B] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToCategoryOps
  object ops {
    implicit def toAllCategoryOps[F[_, _], A, B](target: F[A, B])(implicit tc: Category[F]): AllOps[F, A, B] {
      type TypeClassType = Category[F]
    } = new AllOps[F, A, B] {
      type TypeClassType = Category[F]
      val self: F[A, B] = target
      val typeClassInstance: TypeClassType = tc
    }
  }

  /****************************************************************************
   * END OF SIMULACRUM-MANAGED CODE                                           *
   ****************************************************************************/

}

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

object Profunctor {
  /****************************************************************************
   * THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      *
   ****************************************************************************/

  /**
   * Summon an instance of [[Profunctor]] for `F`.
   */
  @inline def apply[F[_, _]](implicit instance: Profunctor[F]): Profunctor[F] = instance

  trait Ops[F[_, _], A, B] {
    type TypeClassType <: Profunctor[F]
    def self: F[A, B]
    val typeClassInstance: TypeClassType
    def dimap[C, D](f: C => A)(g: B => D): F[C, D] = typeClassInstance.dimap[A, B, C, D](self)(f)(g)
    final def lmap[C](f: C => A): F[C, B] = typeClassInstance.lmap[A, B, C](self)(f)
    @inline final def rmap[C](f: B => C): F[A, C] = typeClassInstance.rmap[A, B, C](self)(f)
  }
  trait AllOps[F[_, _], A, B] extends Ops[F, A, B]
  trait ToProfunctorOps {
    implicit def toProfunctorOps[F[_, _], A, B](target: F[A, B])(implicit tc: Profunctor[F]): Ops[F, A, B] {
      type TypeClassType = Profunctor[F]
    } = new Ops[F, A, B] {
      type TypeClassType = Profunctor[F]
      val self: F[A, B] = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToProfunctorOps
  object ops {
    implicit def toAllProfunctorOps[F[_, _], A, B](target: F[A, B])(implicit tc: Profunctor[F]): AllOps[F, A, B] {
      type TypeClassType = Profunctor[F]
    } = new AllOps[F, A, B] {
      type TypeClassType = Profunctor[F]
      val self: F[A, B] = target
      val typeClassInstance: TypeClassType = tc
    }
  }

  /****************************************************************************
   * END OF SIMULACRUM-MANAGED CODE                                           *
   ****************************************************************************/

}
