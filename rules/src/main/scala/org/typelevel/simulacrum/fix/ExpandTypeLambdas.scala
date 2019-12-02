package org.typelevel.simulacrum.fix

import scalafix.v1.{Patch, SyntacticDocument, SyntacticRule}
import scala.meta.{Name, Term, Transformer, Tree, Type, XtensionQuasiquoteType, XtensionQuasiquoteTypeParam}

class ExpandTypeLambdas extends SyntacticRule("ExpandTypeLambdas") {
  override def description: String = "Expand kind-projector's syntax for type lambdas"
  override def isLinter: Boolean = false

  private val syntheticParams: List[String] = ('α' to 'ω').map(_.toString + "$").toList

  /**
   * We currently special-case the handling of types in polymorphic lambda value definitions.
   */
  private def isInsidePolymorphicLambda(tree: Tree): Boolean = tree.parent match {
    case None                                    => false
    case Some(Term.ApplyType(Term.Name("λ"), _)) => true
    case Some(parent)                            => isInsidePolymorphicLambda(parent)
  }

  private def isInsideTypeLambda(tree: Tree): Boolean = tree.parent match {
    case None                                                                         => false
    case Some(Type.Apply(_, params)) if KindProjector.containsTypeLambdaParam(params) => true
    case Some(parent)                                                                 => isInsideTypeLambda(parent)
  }

  private def notNested(tree: Tree): Boolean = !isInsidePolymorphicLambda(tree) && !isInsideTypeLambda(tree)

  override def fix(implicit doc: SyntacticDocument): Patch = Patch.fromIterable(
    doc.tree.collect {
      case lambda @ Type.Apply(name @ Type.Name("Lambda" | "λ"), List(Type.Function(_, _))) if notNested(lambda) =>
        Some(Patch.replaceTree(lambda, KindProjector.expandTypeLambda(lambda).toString))
      case t: Type if notNested(t) =>
        KindProjector.expandInApplication(t).map(e => Patch.replaceTree(t, e.toString))
      case other => None
    }.flatten
  )
}

object KindProjector {
  object Placeholder {
    def unapply(tpe: Type): Option[Variance] = tpe match {
      case Type.Name("*" | "?")   => Some(Variance.Invariant)
      case Type.Name("-*" | "-?") => Some(Variance.Contravariant)
      case Type.Name("+*" | "+?") => Some(Variance.Covariant)
      case _                      => None
    }
  }

  private val syntheticParams: List[String] = ('α' to 'η').map(_.toString).toList

  def containsTypeLambdaParam(params: List[Type]): Boolean = countTypeLambdaParams(params) > 0

  def countTypeLambdaParams(params: List[Type]): Int = params.count {
    case Type.Name("*" | "?" | "-*" | "-?" | "+*" | "+?") => true
    case _                                                => false
  }

  def makeTparam(name: String, variance: Variance): Type.Param = variance match {
    case Variance.Invariant     => tparam"${Name(name)}"
    case Variance.Contravariant => tparam"-${Name(name)}"
    case Variance.Covariant     => tparam"+${Name(name)}"
  }

  def expandInApplication(apply: Type, level: Int = 1): Option[Type] = {
    val syntheticParamNames = syntheticParams.map(_ + ("$" * level))
    val state = (syntheticParamNames, List.empty[Type.Param], List.empty[Type])

    apply match {
      case Type.Apply(name, params) if containsTypeLambdaParam(params) =>
        val (_, lambdaParams, newParams) =
          params.foldLeft(state) {
            case ((next :: left, lambdaParams, newParams), Placeholder(v)) =>
              (left, lambdaParams :+ makeTparam(next, v), newParams :+ Type.Name(next))
            case ((left, lambdaParams, newParams), param) => (left, lambdaParams, newParams :+ expand(param, level + 1))
          }

        Some(t"({ type λ[..$lambdaParams] = $name[..$newParams] })#λ")
      case Type.Tuple(params) if containsTypeLambdaParam(params) =>
        val (_, lambdaParams, newParams) =
          params.foldLeft(state) {
            case ((next :: left, lambdaParams, newParams), Placeholder(v)) =>
              (left, lambdaParams :+ makeTparam(next, v), newParams :+ Type.Name(next))
            case ((left, lambdaParams, newParams), param) => (left, lambdaParams, newParams :+ expand(param, level + 1))
          }

        Some(t"({ type λ[..$lambdaParams] = (..$newParams) })#λ")
      case Type.Function(List(a), b) if containsTypeLambdaParam(List(a, b)) =>
        val (leftA, lambdaParamsA, newA) = a match {
          case Placeholder(v) =>
            (
              syntheticParamNames.tail,
              makeTparam(syntheticParamNames.head, v) :: Nil,
              Type.Name(syntheticParamNames.head)
            )
          case other => (syntheticParamNames, Nil, other)
        }

        val (lambdaParamsB, newB) = b match {
          case Placeholder(v) =>
            (lambdaParamsA :+ makeTparam(leftA.head, v), Type.Name(leftA.head))
          case other => (lambdaParamsA, other)
        }

        Some(t"({ type λ[..$lambdaParamsB] = $newA => $newB })#λ")
      case Type.ApplyInfix(a, f, b) if containsTypeLambdaParam(List(a, b)) =>
        val (leftA, lambdaParamsA, newA) = a match {
          case Placeholder(v) =>
            (
              syntheticParamNames.tail,
              makeTparam(syntheticParamNames.head, v) :: Nil,
              Type.Name(syntheticParamNames.head)
            )
          case other => (syntheticParamNames, Nil, other)
        }

        val (lambdaParamsB, newB) = b match {
          case Placeholder(v) =>
            (lambdaParamsA :+ makeTparam(leftA.head, v), Type.Name(leftA.head))
          case other => (lambdaParamsA, other)
        }

        Some(t"({ type λ[..$lambdaParamsB] = $newA $f $newB })#λ")

      case other => None
    }
  }

  def expandTypeLambda(lambda: Type.Apply): Type = lambda match {
    case Type.Apply(name @ Type.Name("Lambda" | "λ"), List(Type.Function(args, body))) =>
      val newArgs: List[Type.Param] = args.flatMap {
        case Type.Name(name) if name.startsWith("-") => Some(tparam"-${Name(name.tail)}")
        case Type.Name(name) if name.startsWith("+") => Some(tparam"+${Name(name.tail)}")
        case Type.Name(name)                         => Some(tparam"${Name(name)}")
        case _                                       => None
      }

      t"({ type $name[..$newArgs] = $body })#$name"

    case other => other
  }

  def expand(tpe: Type, level: Int = 1): Type = tpe match {
    case apply @ Type.Apply(_, params) if containsTypeLambdaParam(params) =>
      expandInApplication(apply, level).getOrElse(tpe)
    case lambda @ Type.Apply(Type.Name("Lambda" | "λ"), _) => expandTypeLambda(lambda)
    case Type.Apply(t, params)                             => Type.Apply(t, params.map(expand(_)))
    case other                                             => other
  }

  def apply1(tpe: Type, param: Type): Type = tpe match {
    case apply @ Type.Apply(name, params) if countTypeLambdaParams(params) == 1 =>
      val newParams = params.map {
        case Type.Name("*" | "?") => param
        case other                => other
      }
      Type.Apply(name, newParams)
    case lambda @ Type.Apply(Type.Name("Lambda" | "λ"), List(Type.Function(List(Type.Name(arg)), body))) =>
      val transformer = new Transformer {
        override def apply(tree: Tree): Tree = tree match {
          case Type.Name(`arg`) => param
          case node             => super.apply(node)
        }
      }
      transformer(body).asInstanceOf[Type]

    case other => t"$tpe[$param]"
  }
}
