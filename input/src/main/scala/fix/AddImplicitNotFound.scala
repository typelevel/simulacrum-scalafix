/*
rule = AddImplicitNotFound
 */
package cats

import simulacrum.typeclass

/**
 * Some type class.
 */
@typeclass trait MissingImplicitNotFound[F[_]]

/**
 * Some type class.
 */
@scala.annotation.implicitNotFound("Some garbage here")
@typeclass trait BadImplicitNotFound[F[_]]
