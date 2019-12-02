package org.typelevel.simulacrum.fix

import scalafix.v1.{Patch, SyntacticDocument, SyntacticRule}
import scala.meta.{Term, Token}

/**
 * Dotty doesn't like `.map { i: Int => i }`. This rule parenthesizes these cases.
 */
class ParenthesizeLambdaParamWithType extends SyntacticRule("ParenthesizeLambdaParamWithType") {
  override def description: String = "Parenthesize lambda parameters with types"
  override def isLinter: Boolean = false

  override def fix(implicit doc: SyntacticDocument): Patch = Patch.fromIterable(
    doc.tree.collect {
      case Term.Function(List(param @ Term.Param(_, _, Some(_), None)), _) =>
        param.tokens.headOption match {
          case Some(Token.LeftParen()) => Patch.empty
          case _                       => Patch.replaceTree(param, s"($param)")
        }
    }
  )
}
