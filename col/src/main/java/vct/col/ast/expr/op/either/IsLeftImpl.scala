package vct.col.ast.expr.op.either

import vct.col.ast.{IsLeft, TBool, Type}

trait IsLeftImpl[G] { this: IsLeft[G] =>
  override def t: Type[G] = TBool()
}