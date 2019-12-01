package org.typelevel.simulacrum.fix

import scalafix.v1.{Patch, SyntacticDocument, SyntacticRule}
import scala.meta.{Init, Mod, Type, XtensionQuasiquoteImporter}

class AddImplicitNotFound extends SyntacticRule("AddImplicitNotFound") {
  override def description: String = "Add implicitNotFound annotation to type class traits or classes"
  override def isLinter: Boolean = false

  def generateAnnotation(typeClass: TypeClass): String =
    s"""@implicitNotFound("Could not find an instance of ${typeClass.name} for $${${typeClass.typeParamName}}")"""

  private def isImplicitNotFound(typeTree: Type): Boolean = typeTree match {
    case Type.Select(_, Type.Name("implicitNotFound")) => true
    case Type.Name("implicitNotFound")                 => true
    case _                                             => false
  }

  private val addImportPatch: Patch = Patch.addGlobalImport(importer"scala.annotation.implicitNotFound")

  override def fix(implicit doc: SyntacticDocument): Patch = TypeClass.toPatch(doc.tree) { typeClass =>
    val newAnnotation = generateAnnotation(typeClass)

    val currentAnnotation = typeClass.tree.fold(_.mods, _.mods).collectFirst {
      case annotation @ Mod.Annot(Init(typeTree, _, _)) if isImplicitNotFound(typeTree) =>
        annotation
    }

    val updateAnnotationPatch = currentAnnotation match {
      case None          => Patch.addLeft(typeClass.tree.merge, s"$newAnnotation\n")
      case Some(current) => Patch.replaceTree(current, newAnnotation)
    }

    addImportPatch + updateAnnotationPatch
  }
}
