package vct.col.ast.expr.resource

import vct.col.ast.{Scale, TResource, Type}

trait ScaleImpl[G] { this: Scale[G] =>
  override def t: Type[G] = TResource()
}