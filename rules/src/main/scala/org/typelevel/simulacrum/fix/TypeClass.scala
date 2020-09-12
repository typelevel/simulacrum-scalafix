package org.typelevel.simulacrum.fix

import org.typelevel.simulacrum.fix.AnnotationParser.{TypeClassAnnotation, TypeClassArgs}
import scala.meta.{Defn, Init, Mod, Name, Pkg, Position, Source, Template, Term, Tree, Type}
import scalafix.lint.Diagnostic
import scalafix.Patch

case class TypeClass(
  tree: Either[Defn.Class, Defn.Trait],
  companion: Option[Tree],
  name: String,
  typeParamName: String,
  characterizedKind: List[Variance],
  parents: List[String],
  excludeParents: List[String],
  generateAllOps: Boolean
) {
  def typeParamApplied(args: List[String]): String = if (args.isEmpty) typeParamName
  else
    typeParamName + args.mkString("[", ", ", "]")
}

object TypeClass {
  private object TraitOrClass {
    def unapply(
      tree: Tree
    ): Option[(Either[Defn.Class, Defn.Trait], List[Mod], Type.Name, List[Type.Param], Template)] = tree match {
      case defn @ Defn.Trait(mods, name, tparams, _, template) => Some((Right(defn), mods, name, tparams, template))
      case defn @ Defn.Class(mods, name, tparams, _, template) => Some((Left(defn), mods, name, tparams, template))
      case _                                                   => None
    }
  }

  object PackageAndBody {
    private object PackageParts {
      def unapply(tree: Tree): Option[List[String]] =
        tree match {
          case Term.Select(base, Term.Name(last)) => unapply(base).map(last :: _)
          case Term.Name(p)                       => Some(List(p))
          case _                                  => None
        }
    }

    def unapply(tree: Tree): Option[(List[String], List[Tree])] = tree match {
      case Pkg(Term.Name(name), List(pkg @ Pkg(_, _))) =>
        unapply(pkg).map { case (inner, body) =>
          (name :: inner, body)
        }
      case Pkg(PackageParts(pkg), body) => Some(pkg.reverse, body)
      case _                            => None
    }
  }

  def toPatch(source: Tree)(f: TypeClass => Patch): Patch = Patch.fromIterable(
    find(source).map {
      case Right(typeClass) => f(typeClass)
      case Left(errors)     => Patch.fromIterable(errors.map(Patch.lint))
    }
  )

  def find(source: Tree): List[Result[TypeClass]] =
    source match {
      case Source(List(PackageAndBody(_, body))) =>
        body.flatMap {
          case TraitOrClass(tree, TypeClassAnnotation(annotationArgs), Type.Name(name), tparams, template) =>
            Some(
              AnnotationParser.parseTypeClassArgs(annotationArgs).flatMap {
                case TypeClassArgs(excludeParents, generateAllOps) =>
                  val companion = body.collectFirst { case tree @ Defn.Object(_, Term.Name(`name`), _) =>
                    tree
                  }

                  tparams match {
                    case List(Type.Param(_, Type.Name(typeParamName), ps, _, _, _)) =>
                      ps.traverse { case Type.Param(mods, Name.Anonymous(), Nil, Type.Bounds(None, None), Nil, Nil) =>
                        mods match {
                          case Nil                       => Right(Variance.Invariant)
                          case List(Mod.Covariant())     => Right(Variance.Covariant)
                          case List(Mod.Contravariant()) => Right(Variance.Contravariant)
                          case other =>
                            Left(
                              other.filter {
                                case Mod.Contravariant() => false
                                case Mod.Covariant()     => false
                                case _                   => true
                              }.map(mod => TypeParamsError(mod.pos))
                            )
                        }
                      }.flatMap { characterizedKinds =>
                        val parents = template.inits.collect {
                          case Init(Type.Apply(Type.Name(name), List(Type.Name(`typeParamName`))), _, _) => name
                        }

                        Right(
                          TypeClass(
                            tree,
                            companion,
                            name,
                            typeParamName,
                            characterizedKinds,
                            parents,
                            excludeParents,
                            generateAllOps
                          )
                        )
                      }
                    case other => Left(List(TypeParamsError(tree.merge.pos)))
                  }
              }
            )
          case other => None
        }
      case _ => Nil
    }

  case class TypeParamsError(position: Position) extends Diagnostic {
    def message: String = "Type class trait should have a single type parameter"
  }
}
