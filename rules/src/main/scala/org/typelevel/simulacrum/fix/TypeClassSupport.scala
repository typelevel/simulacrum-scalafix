package org.typelevel.simulacrum.fix

import metaconfig.{ConfDecoder, Configured}
import metaconfig.generic.{Surface, deriveDecoder, deriveSurface}
import scalafix.v1.{Configuration, Patch, Rule, SyntacticDocument, SyntacticRule}
import scala.meta.{Decl, Defn, Mod, Term, Token, Transformer, Type, XtensionQuasiquoteTerm, XtensionQuasiquoteType}
import scala.meta._

case class Deprecation(message: String, since: String)
case class TypeClassSupportConfig(opsObjectDeprecation: Option[Deprecation])

object TypeClassSupportConfig {
  val default: TypeClassSupportConfig = TypeClassSupportConfig(None)

  implicit val deprecationSurface: Surface[Deprecation] = deriveSurface
  implicit val deprecationDecoder: ConfDecoder[Deprecation] = deriveDecoder(Deprecation("", ""))

  implicit val configSurface: Surface[TypeClassSupportConfig] = deriveSurface
  implicit val configDecoder: ConfDecoder[TypeClassSupportConfig] = deriveDecoder(default)
}

class TypeClassSupport(config: TypeClassSupportConfig) extends SyntacticRule("TypeClassSupport") {
  def this() = this(TypeClassSupportConfig.default)

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf.getOrElse("TypeClassSupport")(this.config).map(new TypeClassSupport(_))

  override def description: String = "Add summoner and syntax method support to type class companion object"
  override def isLinter: Boolean = false

  def banner: String =
    """|  /* ======================================================================== */
       |  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
       |  /* ======================================================================== */
       |""".stripMargin

  def footer: String =
    """|  /* ======================================================================== */
       |  /* END OF SIMULACRUM-MANAGED CODE                                           */
       |  /* ======================================================================== */
       |""".stripMargin

  private def getComments(input: String): (String, String, String) =
    input.split("\n") match {
      case Array(i0, i1, i2) => (i0.drop(4).dropRight(2), i1.drop(4).dropRight(2), i2.drop(4).dropRight(2))
    }
  def isBanner(c0: String, c1: String, c2: String): Boolean = (c0, c1, c2) == getComments(banner)
  def isFooter(c0: String, c1: String, c2: String): Boolean = (c0, c1, c2) == getComments(footer)

  def isValidOpsMethod(mods: List[Mod]): Boolean = mods.forall {
    case Mod.Private(_)   => false
    case Mod.Protected(_) => false
    case Mod.Override()   => false
    case _                => true
  }

  private def getAllTypeNames(tree: Type): List[Type.Name] = tree match {
    case name @ Type.Name(_)           => List(name)
    case Type.Apply(_, params)         => params.flatMap(getAllTypeNames)
    case Type.Function(params, result) => (params :+ result).flatMap(getAllTypeNames)
    case _                             => Nil
  }

  private val genericNames: List[String] = ('A' to 'Z').map(_.toString).toList

  class TypeNameTransformer(f: String => String) extends Transformer {
    def transformType(value: Type): Type = apply(value).asInstanceOf[Type]
    def transformTerm(value: Term): Term = apply(value).asInstanceOf[Term]
    def transformTypeParam(value: Type.Param): Type.Param = apply(value).asInstanceOf[Type.Param]
    def transformTermParam(value: Term.Param): Term.Param = apply(value).asInstanceOf[Term.Param]

    override def apply(tree: Tree): Tree = tree match {
      case t @ Type.Name(n) => Type.Name(f(n))
      case node             => super.apply(node)
    }
  }

  def addToImplicitParams(
    paramss: List[List[Term.Param]],
    param: Term.Param,
    atEnd: Boolean = true
  ): List[List[Term.Param]] = {
    val hasImplicitParams = paramss.lastOption
      .flatMap(_.headOption)
      .exists(
        _.mods.exists {
          case Mod.Implicit() => true
          case _              => false
        }
      )

    if (hasImplicitParams)
      paramss.init :+ (if (atEnd) (paramss.last :+ param) else (param :: paramss.last))
    else
      paramss :+ List(param)
  }

  private def newParams(
    typeClass: TypeClass,
    tparams: List[Type.Param],
    selfParam: Term.Param,
    params: List[Term.Param],
    otherParamss: List[List[Term.Param]]
  ): Option[
    (
      List[Type.Param],
      List[List[Term.Param]],
      List[Type],
      List[List[Term]],
      TypeNameTransformer
    )
  ] = selfParam match {
    case Term.Param(Nil, Term.Name(_), Some(selfParamType), None) =>
      val typeParams = selfParamType match {
        case Type.Name(tpn) if tpn == typeClass.typeParamName                 => Some(Nil)
        case Type.Apply(Type.Name(tpn), ps) if tpn == typeClass.typeParamName => Some(ps)
        case _                                                                => None
      }

      typeParams.flatMap { ps =>
        val (leftTparams, leftGenericNames, mapping, evs, cbsx) =
          ps.foldLeft(
            (
              tparams,
              genericNames,
              Map.empty[String, String],
              List.empty[(Type.Name, Type)],
              List.empty[Type]
            )
          ) {
            case ((currentTparams, gn :: gns, mapping, evs, cbs), Type.Name(name)) =>
              val (usedTparams, newTparams) = currentTparams.partition(_.name.value == name)

              (newTparams, gns, mapping.updated(name, gn), evs, cbs)
            case ((currentTparams, gn :: gns, mapping, evs, cbs), other) =>
              val allTypeNames = getAllTypeNames(other)
              val newTparams = currentTparams.filterNot(param => allTypeNames.contains(param.name.value))
              (newTparams, gns, mapping, evs :+ (Type.Name(gn) -> other), cbs)
          }

        val cbs = tparams.flatMap {
          case param @ Type.Param(_, name, _, _, _, cbounds) =>
            cbounds.map { cbound =>
              t"$cbound[${Type.Name(param.name.value)}]"
            }
        }

        val fullMapping = mapping ++ leftTparams.filter(_.tparams.isEmpty).zip(leftGenericNames).map {
          case (param, name) => (param.name.value, name)
        }

        val transform: String => String = name => fullMapping.getOrElse(name, name)
        val transformer = new TypeNameTransformer(transform)

        val newParamss = params match {
          case Nil  => otherParamss
          case rest => rest :: otherParamss
        }

        val evParams = evs.zipWithIndex.map {
          case ((source, target), i) =>
            val paramName = Term.Name(s"ev$$${i + 1}")
            param"implicit $paramName: $source <:< ${transformer.transformType(target)}"
        }

        val cbParams = cbs.zipWithIndex.map {
          case (target, i) =>
            val paramName = Term.Name(s"ev$$${evParams.size + i + 1}")
            param"implicit $paramName: ${transformer.transformType(target)}"
        }

        val mappedParamss = cbParams.reverse.foldLeft(
          evParams.reverse.foldLeft(
            newParamss.map(_.map(transformer.transformTermParam))
          )(addToImplicitParams(_, _, false))
        )(addToImplicitParams(_, _, true))

        val appliedTparams = tparams.map(param => Type.Name(param.name.value))

        val selfParam = evs match {
          case Nil => Term.Name("self")
          case targets =>
            q"self.asInstanceOf[${Type.Name(typeClass.typeParamName)}[..${targets.map(p => transformer.transformType(p._2))}]]"
        }

        val appliedParamss = (selfParam :: params.map(TypeClassSupport.toTermName)) ::
          otherParamss.map(_.map(TypeClassSupport.toTermName))

        Some(
          (
            leftTparams.map(param => transformer.transformTypeParam(param).copy(cbounds = Nil)),
            mappedParamss,
            appliedTparams.map(transformer.transformType),
            appliedParamss,
            transformer
          )
        )
      }

    case _ => None
  }

  def toSyntaxMethod(typeClass: TypeClass, method: TypeClassSupport.MethodInfo): List[Defn.Def] =
    method match {
      case TypeClassSupport.MethodInfo(mods, origName, tparams, (selfParam :: params) :: otherParamss, decltpe)
          if isValidOpsMethod(mods) =>
        val names: List[Term.Name] = mods match {
          case AnnotationParser.NoopAnnotation(_) => Nil
          case AnnotationParser.OpAnnotation(args) =>
            AnnotationParser.parseOpArgs(args, typeClass.tree.merge) match {
              case Left(_)                                       => Nil
              case Right(AnnotationParser.OpArgs(opName, true))  => List(origName, Term.Name(opName))
              case Right(AnnotationParser.OpArgs(opName, false)) => List(Term.Name(opName))
            }
          case _ => List(origName)
        }

        val newMods = mods.filterNot(AnnotationParser.isMethodAnnotation)

        newParams(typeClass, tparams, selfParam, params, otherParamss) match {
          case Some((newTparams, newParamss, appliedTparams, appliedParamss, transformer)) =>
            val body =
              if (appliedTparams.isEmpty)
                q"typeClassInstance.$origName(...$appliedParamss)"
              else
                q"typeClassInstance.$origName[..$appliedTparams](...$appliedParamss)"

            names.map { name =>
              Defn.Def(newMods, name, newTparams, newParamss, decltpe.map(transformer.transformType), body)
            }

          case None => Nil
        }

      case TypeClassSupport.MethodInfo(_, _, _, _, _) => Nil
    }

  def generateOpsCode(typeClass: TypeClass, methods: List[Defn.Def]): String = {
    val params: List[String] = ('A' to 'Z').map(_.toString).take(typeClass.characterizedKind.size).toList

    val Name = typeClass.name
    val TypeParamName = typeClass.typeParamName
    val InstanceType = s"$Name[$TypeParamName]"
    val TypeParamDecl = typeClass.typeParamApplied(typeClass.characterizedKind.map(_ => "_"))
    val TypeParamsDecl = TypeParamDecl + params.map(", " + _).mkString
    val TypeParamsArgs = TypeParamName + params.map(", " + _).mkString
    val ValueType = if (params.isEmpty) TypeParamName else s"$TypeParamName[${params.mkString(", ")}]"
    val Methods = methods.map("\n    " + _).mkString

    val AllOpsParents = typeClass.parents.filterNot(typeClass.excludeParents.contains) match {
      case Nil => ""
      case parents =>
        parents.map { parents =>
          s" with $parents.AllOps[$TypeParamsArgs]"
        }.mkString
    }

    val AllOpsBody = typeClass.parents match {
      case Nil => ""
      case _ =>
        s"""| {
            |    type TypeClassType <: $InstanceType
            |  }""".stripMargin
    }

    val deprecation = config.opsObjectDeprecation match {
      case Some(Deprecation(message, since)) => "@deprecated(\"" + message + "\", \"" + since + "\")\n  "
      case None                              => ""
    }

    s"""|  /**
        |   * Summon an instance of [[$Name]] for `$TypeParamName`.
        |   */
        |  @inline def apply[$TypeParamDecl](implicit instance: $InstanceType): $InstanceType = instance
        |
        |  trait Ops[$TypeParamsDecl] extends Serializable {
        |    type TypeClassType <: $InstanceType
        |    def self: $ValueType
        |    val typeClassInstance: TypeClassType$Methods
        |  }
        |  trait AllOps[$TypeParamsDecl] extends Ops[$TypeParamsArgs]$AllOpsParents$AllOpsBody
        |  trait To${Name}Ops extends Serializable {
        |    implicit def to${Name}Ops[$TypeParamsDecl](target: $ValueType)(implicit tc: $InstanceType): Ops[$TypeParamsArgs] {
        |      type TypeClassType = $InstanceType
        |    } = new Ops[$TypeParamsArgs] {
        |      type TypeClassType = $InstanceType
        |      val self: $ValueType = target
        |      val typeClassInstance: TypeClassType = tc
        |    }
        |  }
        |  ${deprecation}object nonInheritedOps extends To${Name}Ops
        |  ${deprecation}object ops {
        |    implicit def toAll${Name}Ops[$TypeParamsDecl](target: $ValueType)(implicit tc: $InstanceType): AllOps[$TypeParamsArgs] {
        |      type TypeClassType = $InstanceType
        |    } = new AllOps[$TypeParamsArgs] {
        |      type TypeClassType = $InstanceType
        |      val self: $ValueType = target
        |      val typeClassInstance: TypeClassType = tc
        |    }
        |  }""".stripMargin
  }

  override def fix(implicit doc: SyntacticDocument): Patch = TypeClass.toPatch(doc.tree) { typeClass =>
    val template = typeClass.tree.fold(_.templ, _.templ)
    val stats = template.stats

    val methods = stats.flatMap {
      case decl @ Decl.Def(_, _, _, _, _)    => toSyntaxMethod(typeClass, TypeClassSupport.declToMethodInfo(decl))
      case defn @ Defn.Def(_, _, _, _, _, _) => toSyntaxMethod(typeClass, TypeClassSupport.defnToMethodInfo(defn))
      case _                                 => None
    }

    val code = generateOpsCode(typeClass, methods)

    typeClass.companion match {
      case None =>
        val companionString =
          s"""|
                    |
                    |object ${typeClass.name} {
                    |$banner
                    |$code
                    |
                    |$footer
                    |}""".stripMargin

        Patch.addRight(typeClass.tree.merge, companionString)
      case Some(currentCompanion) =>
        val tokens = currentCompanion.tokens.toList

        object ThreeComments {
          def unapply(tokens: List[Token]): Option[(String, String, String)] = tokens match {
            case List(
                  Token.Comment(c0),
                  Token.LF(),
                  Token.Space(),
                  Token.Space(),
                  Token.Comment(c1),
                  Token.LF(),
                  Token.Space(),
                  Token.Space(),
                  Token.Comment(c2)
                ) =>
              Some((c0, c1, c2))
            case _ => None
          }
        }

        // Working around scala.meta #1913 (bug in `span`).
        val bannerIndex = tokens.sliding(9).indexWhere {
          case ThreeComments(c0, c1, c2) => isBanner(c0, c1, c2)
          case _                         => false
        }
        val footerIndex = tokens.sliding(9).indexWhere {
          case ThreeComments(c0, c1, c2) => isFooter(c0, c1, c2)
          case _                         => false
        }

        if (bannerIndex < 0 || footerIndex < 0)
          currentCompanion.tokens.last match {
            case brace @ Token.RightBrace() => Patch.addLeft(brace, s"\n$banner\n$code\n\n$footer\n")
            case other                      => Patch.addRight(other, s" {\n$banner\n$code\n\n$footer\n}")
          }
        else
          Patch.removeTokens(tokens.slice(bannerIndex, footerIndex + 9)) +
            Patch.addRight(
              currentCompanion.tokens.apply(bannerIndex - 1),
              s"${banner.dropWhile(_ == ' ')}\n$code\n\n$footer"
            )
    }
  }
}

object TypeClassSupport {
  private case class MethodInfo(
    mods: List[Mod],
    name: Term.Name,
    tparams: List[Type.Param],
    paramss: List[List[Term.Param]],
    decltpe: Option[Type]
  )

  private def declToMethodInfo(decl: Decl.Def): MethodInfo =
    MethodInfo(decl.mods, decl.name, decl.tparams, decl.paramss, Some(decl.decltpe))

  private def defnToMethodInfo(defn: Defn.Def): MethodInfo =
    MethodInfo(defn.mods, defn.name, defn.tparams, defn.paramss, defn.decltpe)

  private def toTermName(p: Term.Param): Term.Name = Term.Name(p.name.value)
  private def toTypeName(p: Type.Param): Type.Name = Type.Name(p.name.value)
}
