package vct.col.ast.temporaryimplpackage.expr.ambiguous

import vct.col.ast._
import vct.col.coerce.CoercionUtils

trait AmbiguousMultImpl[G] { this: AmbiguousMult[G] =>
  def isProcessOp: Boolean = CoercionUtils.getCoercion(left.t, TProcess()).isDefined
  def isIntOp: Boolean = CoercionUtils.getCoercion(left.t, TInt()).isDefined && CoercionUtils.getCoercion(right.t, TInt()).isDefined
  def isFloatOp: Boolean = CoercionUtils.getCoercion(left.t, TFloat()).isDefined && CoercionUtils.getCoercion(right.t, TFloat()).isDefined
  def isSetOp: Boolean = CoercionUtils.getAnySetCoercion(left.t).isDefined
  def isBagOp: Boolean = CoercionUtils.getAnyBagCoercion(left.t).isDefined

  override def t: Type[G] =
    if(isProcessOp) TProcess()
    else (if(isIntOp) TInt()
    else if (isFloatOp) TFloat()
    else TRational())
}