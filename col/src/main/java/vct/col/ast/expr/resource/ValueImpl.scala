package vct.col.ast.expr.resource

import vct.col.ast.{TResource, Type, Value}

trait ValueImpl[G] { this: Value[G] =>
  override def t: Type[G] = TResource()

}
