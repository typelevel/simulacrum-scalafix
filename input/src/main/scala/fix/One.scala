/*
rules = [
  AddSerializable,
  AddImplicitNotFound,
  TypeClassSupport
]
 */
package alleycats

import cats.kernel.Eq
import simulacrum.typeclass

@typeclass trait One[A] extends Serializable {
  def one: A

  def isOne(a: A)(implicit ev: Eq[A]): Boolean = ev.eqv(one, a)
}

object One {
  def apply[A](a: => A): One[A] =
    new One[A] { lazy val one: A = a }
}
