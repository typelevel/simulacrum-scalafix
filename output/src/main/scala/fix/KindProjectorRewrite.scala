package example

class Foo[A, B]
class Bar[F[_]]

class Baz extends Bar[({ type λ[α$] = Foo[α$, Int] })#λ]
class Zab extends Bar[({ type λ[α$] = Int Foo α$ })#λ]
class Tpl extends Bar[({ type λ[α$] = (α$, Int) })#λ]
class Fnc extends Bar[({ type λ[α$] = α$ => Int })#λ]
class Qux extends Bar[({ type λ[P] = (P, P) })#λ]
class Xuq extends Bar[({ type λ[-x] = x => Int })#λ]

class Both[F[_], A]
class Nested extends Bar[({ type λ[α$] = Both[({ type λ[α$$] = Foo[Int, α$$] })#λ, α$] })#λ]

class EitherK[F[_], G[_], A]

trait FuncK[F[_], G[_]] {
  def apply[A](fa: F[A]): G[A]
}

object FuncK {
  type ~>[F[_], G[_]] = FuncK[F, G]

  val fk1 = new FuncK[Option, List] { def apply[A$](a$: Option[A$]): List[A$] = a$.toList }
  val fk2 = new FuncK[Option, List] { def apply[A$](a$: Option[A$]): List[A$] = Nil }
  val fk3 = new (List ~> Option) { def apply[A$](a$: List[A$]): Option[A$] = a$.headOption }
  val fk4 = new (List ~> Option) { def apply[A$](xs: List[A$]): Option[A$] = xs.lastOption }
  val fk5 = new (({ type λ[α$] = Foo[α$, Int] })#λ ~> Option) { def apply[A$](a$: Foo[A$, Int]): Option[A$] = None }
  val fk6 = new FuncK[List, ({ type λ[x] = List[List[x]] })#λ] { def apply[A$](a$: List[A$]): List[List[A$]] = List(a$) }
  val fk7 = new (List ~> Option) { def apply[A$](a$: List[A$]): Option[A$] = a$.headOption.map(identity) }
  val fk8 = new (List ~> Option) { def apply[A$](list: List[A$]): Option[A$] = {
    val intermediate = list.headOption
    intermediate
  } }
}
