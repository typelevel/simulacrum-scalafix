package cats

import simulacrum.typeclass
import scala.annotation.implicitNotFound

/**
 * Some type class.
 */
@implicitNotFound("Could not find an instance of MissingImplicitNotFound for ${F}")
@typeclass trait MissingImplicitNotFound[F[_]]

/**
 * Some type class.
 */
@implicitNotFound("Could not find an instance of BadImplicitNotFound for ${F}")
@typeclass trait BadImplicitNotFound[F[_]]
