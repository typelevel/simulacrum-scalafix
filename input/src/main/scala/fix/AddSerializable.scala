/*
rule = AddSerializable
 */
package cats

@simulacrum.typeclass
trait MissingSerializable[F[_]]

@simulacrum.typeclass
trait MissingSerializableWithBody[F[_]] {
  def foo: String = ""
}

@simulacrum.typeclass
trait NotMissingSerializable[F[_]] extends Serializable

@simulacrum.typeclass
trait ParentWithSerializable[F[_]] extends Serializable

@simulacrum.typeclass
trait NotMissingSerializableViaParent[F[_]] extends ParentWithSerializable[F]
