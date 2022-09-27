package vct.col.ast.expr.resource

import vct.col.ast.{TBool, Type, ValidMatrix}

trait ValidMatrixImpl[G] { this: ValidMatrix[G] =>
  override def t: Type[G] = TBool()
}