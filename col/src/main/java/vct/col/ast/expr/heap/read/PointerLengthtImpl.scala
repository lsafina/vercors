package vct.col.ast.expr.heap.read

import vct.col.ast.{PointerLength, TInt, Type}

trait PointerLengthtImpl[G] { this: PointerLength[G] =>
  override def t: Type[G] = TInt()
}
