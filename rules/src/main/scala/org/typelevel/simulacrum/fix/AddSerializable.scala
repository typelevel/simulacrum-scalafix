package org.typelevel.simulacrum.fix

import scalafix.v1.{Patch, SyntacticDocument, SyntacticRule}
import scala.meta.{Template, Token}

/**
 * @note This rule assumes that if the type class trait or class has at least one parent, it already extends Serializable.
 */
class AddSerializable extends SyntacticRule("AddSerializable") {
  override def description: String = "Add Serializable"
  override def isLinter: Boolean = false

  private val code: String = "extends Serializable"

  override def fix(implicit doc: SyntacticDocument): Patch = TypeClass.toPatch(doc.tree) { typeClass =>
    typeClass.tree.fold(_.templ, _.templ) match {
      case tree @ Template(_, Nil, self, _) =>
        val tokens = typeClass.tree.merge.tokens
        val openingBrace = tokens.collectFirst {
          case brace @ Token.LeftBrace() => brace
        }

        openingBrace match {
          case Some(brace) => Patch.addLeft(brace, s"$code ")
          case None        => Patch.addRight(tokens.last, s" $code")
        }
      case _ => Patch.empty
    }
  }
}
