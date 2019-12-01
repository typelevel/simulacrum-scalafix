package org.typelevel.simulacrum

import scalafix.lint.Diagnostic

package object fix {
  type Result[+A] = Either[List[Diagnostic], A]

  implicit class ResultTraverseOps[A](values: List[A]) {
    def traverse[B](f: A => Result[B]): Result[List[B]] =
      values
        .foldLeft[Either[List[Diagnostic], List[B]]](Right(Nil)) {
          case (Left(errors), a) =>
            f(a) match {
              case Right(_)        => Left(errors)
              case Left(newErrors) => Left(errors ++ newErrors)
            }
          case (Right(bs), a) => f(a).map(_ :: bs)
        }
        .map(_.reverse)
  }

  implicit class ResultOps[A](result: Result[A]) {
    def map2[B, C](other: Result[B])(f: (A, B) => C): Result[C] = result match {
      case Right(value) =>
        other match {
          case Right(otherValue) => Right(f(value, otherValue))
          case Left(otherErrors) => Left(otherErrors)
        }
      case Left(errors) =>
        other match {
          case Left(otherErrors) => Left(errors ++ otherErrors)
          case Right(_)          => Left(errors)
        }
    }
  }
}
