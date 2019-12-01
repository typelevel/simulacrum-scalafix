package org.typelevel.simulacrum.fix

import scala.meta.{Init, Lit, Mod, Name, Position, Term, Tree, Type}
import scalafix.lint.Diagnostic

object AnnotationParser {
  object TypeClassAnnotation extends AnnotationPattern("typeclass")
  object OpAnnotation extends AnnotationPattern("op")
  object NoopAnnotation extends AnnotationPattern("noop")

  case class TypeClassArgs(excludeParents: List[String] = Nil, generateAllOps: Boolean = true)
  case class OpArgs(name: String, alias: Boolean = false)

  def parseTypeClassArgs(args: List[Term]): Result[TypeClassArgs] = args match {
    case Nil => Right(TypeClassArgs())
    case List(arg) =>
      parseExcludeParentsArg(arg).map(TypeClassArgs(_)) match {
        case Left(_) => parseGenerateAllOpsArg(arg).map(value => TypeClassArgs(generateAllOps = value))
        case success => success
      }

    case List(arg1, arg2) =>
      parseExcludeParentsArg(arg1).map2(parseGenerateAllOpsArg(arg2))(TypeClassArgs(_, _)) match {
        case Left(_) => parseExcludeParentsArg(arg2).map2(parseGenerateAllOpsArg(arg1))(TypeClassArgs(_, _))
        case success => success
      }
    case other :: _ => argumentError(other, "typeclass")
  }

  def parseOpArgs(args: List[Term], fallback: Tree): Result[OpArgs] = args match {
    case List(arg) => parseNameArg(arg).map(OpArgs(_))
    case List(arg1, arg2) =>
      parseNameArg(arg1).map2(parseAliasArg(arg2))(OpArgs(_, _)) match {
        case Left(_) => parseNameArg(arg2).map2(parseAliasArg(arg1))(OpArgs(_, _))
        case success => success
      }
    case Nil        => argumentError(fallback, "op")
    case other :: _ => argumentError(other, "op")
  }

  private def isAnnotationNamed(name: String)(typeTree: Type): Boolean = typeTree match {
    case Type.Select(_, Type.Name(`name`)) => true
    case Type.Name(`name`)                 => true
    case _                                 => false
  }

  class AnnotationPattern(name: String) {
    def unapply(mods: List[Mod]): Option[List[Term]] = mods.reverse.collectFirst {
      case Mod.Annot(Init(typeTree, Name(""), args)) if isAnnotationNamed(name)(typeTree) => Some(args.flatten)
    }.flatten
  }

  def isMethodAnnotation(mod: Mod): Boolean = mod match {
    case Mod.Annot(Init(typeTree, Name(""), args)) =>
      isAnnotationNamed("noop")(typeTree) || isAnnotationNamed("op")(typeTree)
    case _ => false
  }

  private class NamedArg(name: String) {
    def unapply(arg: Term): Option[Term] = arg match {
      case Term.Assign(Term.Name(`name`), value) => Some(value)
      case _                                     => None
    }
  }

  private object ExcludeParentsArg extends NamedArg("excludeParents")
  private object GenerateAllOpsArg extends NamedArg("generateAllOps")
  private object NameArg extends NamedArg("name")
  private object AliasArg extends NamedArg("alias")

  private def parseExcludeParentsArg(arg: Term): Result[List[String]] = arg match {
    case ExcludeParentsArg(value) => parseExcludeParentsArg(value)
    case Term.Apply(Term.Name("List"), parents) =>
      parents.traverse {
        case Lit.String(parent) => Right(parent)
        case other              => argumentError(other, "typeclass", Some("excludeParents"))
      }
    case other => argumentError(other, "typeclass", Some("excludeParents"))
  }

  private def parseGenerateAllOpsArg(arg: Term): Result[Boolean] = arg match {
    case GenerateAllOpsArg(Lit.Boolean(value)) => Right(value)
    case Lit.Boolean(value)                    => Right(value)
    case other                                 => argumentError(other, "typeclass", Some("generateAllOps"))
  }

  private def parseNameArg(arg: Term): Result[String] = arg match {
    case NameArg(Lit.String(value)) => Right(value)
    case Lit.String(value)          => Right(value)
    case other                      => argumentError(other, "op", Some("name"))
  }

  private def parseAliasArg(arg: Term): Result[Boolean] = arg match {
    case AliasArg(Lit.Boolean(value)) => Right(value)
    case Lit.Boolean(value)           => Right(value)
    case other                        => argumentError(other, "op", Some("alias"))
  }

  private def argumentError(tree: Tree, annotation: String, argument: Option[String] = None): Result[Nothing] =
    Left(List(ArgumentError(tree.pos, annotation, argument)))

  case class ArgumentError(position: Position, annotation: String, argument: Option[String]) extends Diagnostic {
    def message: String = s"Invalid ${argument.fold("")(_ + " ")}argument for $annotation annotation"
  }
}
