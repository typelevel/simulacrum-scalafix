package org.typelevel.simulacrum.fix

import scalafix.v1.{Patch, SyntacticDocument, SyntacticRule}
import scala.meta.{Name, Term, Type}

class ExpandPolymorphicLambdas extends SyntacticRule("ExpandPolymorphicLambdas") {
  override def description: String = "Expand kind-projector's syntax for polymorphic lambda values"
  override def isLinter: Boolean = false

  private val syntheticParams: List[String] = ('α' to 'η').map(_.toString + "$").toList

  private def replacePlaceholder(tree: Term, param: Term.Name): Option[Term] = tree match {
    case Term.Select(Term.Placeholder(), method) => Some(Term.Select(param, method))
    case Term.Select(select @ Term.Select(_, _), method) =>
      replacePlaceholder(select, param).map(select => Term.Select(select, method))
    case Term.Apply(select @ Term.Select(_, _), args) =>
      replacePlaceholder(select, param).map(select => Term.Apply(select, args))
    case other => None
  }

  override def fix(implicit doc: SyntacticDocument): Patch = Patch.fromIterable(
    doc.tree.collect {
      case lambda @ Term.Apply(Term.ApplyType(name @ Term.Name("Lambda" | "λ"), List(funcK)), body) =>
        val parts = funcK match {
          case Type.ApplyInfix(f, k, g)  => Some((k, f, g, true))
          case Type.Apply(k, List(f, g)) => Some((k, f, g, false))
          case _                         => None
        }

        val typeParam = Type.Name("A$")
        val termParam = Term.Name("a$")

        val nameAndBody = body match {
          case List(Term.Function(List(Term.Param(Nil, Name(""), None, None)), body)) => Some((None, body.toString))
          case List(Term.Function(List(Term.Param(Nil, name, None, None)), body))     => Some((Some(name), body.toString))
          case List(Term.Block(List(Term.Function(List(Term.Param(Nil, name, None, None)), body)))) =>
            Some((Some(name), s"{\n    $body\n  }"))
          case List(Term.Apply(method, List(Term.Placeholder()))) => Some((None, s"$method($termParam)"))
          case List(other) =>
            println(other.structure); replacePlaceholder(other, termParam).map(term => (None, term.toString))
          case other => None
        }

        parts match {
          case Some((k, f, g, isInfix)) =>
            nameAndBody match {
              case Some((extractedParamName, newBody)) =>
                val newF = KindProjector.expand(f)
                val newG = KindProjector.expand(g)
                val appliedF = KindProjector.expand(KindProjector.apply1(f, typeParam))
                val appliedG = KindProjector.expand(KindProjector.apply1(g, typeParam))

                val instance = if (isInfix) s"($newF $k $newG)" else s"$k[$newF, $newG]"
                val paramName = extractedParamName.getOrElse(termParam.toString)

                Patch.replaceTree(
                  lambda,
                  s"new $instance { def apply[$typeParam]($paramName: $appliedF): $appliedG = $newBody }"
                )

              case None => Patch.empty
            }
          case None => Patch.empty
        }
    }
  )
}
