package vct.col.ast.expr.sideeffect

import vct.col.ast.{Then, Type}

trait ThenImpl[G] { this: Then[G] =>
  override def t: Type[G] = value.t
}