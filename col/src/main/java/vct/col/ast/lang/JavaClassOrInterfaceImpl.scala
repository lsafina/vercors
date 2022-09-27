package vct.col.ast.lang

import vct.col.ast.{ClassDeclaration, Declaration, JavaClassOrInterface, JavaModifier, JavaTClass, Type, Variable}
import vct.col.ref.Ref
import vct.result.VerificationError.Unreachable

trait JavaClassOrInterfaceImpl[G] { this: JavaClassOrInterface[G] =>
  def name: String
  def modifiers: Seq[JavaModifier[G]]
  def typeParams: Seq[Variable[G]]
  def decls: Seq[ClassDeclaration[G]]
  def supports: Seq[Type[G]]

  def transSupportArrows(seen: Set[JavaClassOrInterface[G]]): Seq[(JavaClassOrInterface[G], JavaClassOrInterface[G])] = {
    if(seen.contains(this)) {
      Nil
    } else {
      val ts = supports.flatMap {
        case JavaTClass(Ref(cls), _) => Seq(cls)
        case _ => Nil
      }

      ts.map((this, _)) ++ ts.flatMap(_.transSupportArrows(Set(this) ++ seen))
    }
  }

  override def declarations: Seq[Declaration[G]] = typeParams ++ decls
}