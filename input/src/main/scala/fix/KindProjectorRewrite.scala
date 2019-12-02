/*
rules = [
  ExpandTypeLambdas,
  ExpandPolymorphicLambdas
]
 */
package example

class Foo[A, B]
class Bar[F[_]]

class Baz extends Bar[Foo[*, Int]]
class Zab extends Bar[Int Foo *]
class Tpl extends Bar[(*, Int)]
class Fnc extends Bar[* => Int]
class Qux extends Bar[λ[P => (P, P)]]
class Xuq extends Bar[λ[`-x` => (x => Int)]]

class Both[F[_], A]
class Nested extends Bar[Both[Foo[Int, *], *]]

class EitherK[F[_], G[_], A]

trait FuncK[F[_], G[_]] {
  def apply[A](fa: F[A]): G[A]
}

object FuncK {
  type ~>[F[_], G[_]] = FuncK[F, G]

  val fk1 = λ[FuncK[Option, List]](_.toList)
  val fk2 = λ[FuncK[Option, List]](_ => Nil)
  val fk3 = λ[List ~> Option](_.headOption)
  val fk4 = λ[List ~> Option](xs => xs.lastOption)
  val fk5 = λ[Foo[*, Int] ~> Option](_ => None)
  val fk6 = λ[FuncK[List, λ[x => List[List[x]]]]](List(_))
  val fk7 = λ[List ~> Option](_.headOption.map(identity))
  val fk8 = λ[List ~> Option] { list =>
    val intermediate = list.headOption
    intermediate
  }
}
