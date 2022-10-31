package vct.col.ast.lang

import vct.col.ast.{SilverCurFieldPerm, TRational, Type}

trait SilverCurFieldPermImpl[G] { this: SilverCurFieldPerm[G] =>
  override def t: Type[G] = TRational()
}