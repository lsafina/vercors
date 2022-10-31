package vct.col.ast.expr.heap.read

import vct.col.ast.expr.ExprImpl
import vct.col.ast.{Deref, TClass, Type}
import vct.col.check.{Check, CheckContext, CheckError}

trait DerefImpl[G] extends ExprImpl[G] { this: Deref[G] =>
  override def t: Type[G] = ref.decl.t
  override def check(context: CheckContext[G]): Seq[CheckError] =
    Check.inOrder(
      super.check(context),
      obj.t.asClass.get.cls.decl.checkDefines(ref.decl, this)
    )
}