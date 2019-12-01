package cats

@simulacrum.typeclass
trait MissingSerializable[F[_]] extends Serializable

@simulacrum.typeclass
trait MissingSerializableWithBody[F[_]] extends Serializable {
  def foo: String = ""
}

@simulacrum.typeclass
trait NotMissingSerializable[F[_]] extends Serializable

@simulacrum.typeclass
trait ParentWithSerializable[F[_]] extends Serializable

@simulacrum.typeclass
trait NotMissingSerializableViaParent[F[_]] extends ParentWithSerializable[F]
