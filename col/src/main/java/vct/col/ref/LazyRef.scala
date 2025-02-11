package vct.col.ref

import vct.col.ast.Declaration
import vct.col.err.MistypedRef
import vct.col.ref

import scala.reflect.ClassTag

class LazyRef[G, Decl <: Declaration[G]](lazyDecl: => Declaration[G], val eqMeasure: Option[Any] = None)(implicit tag: ClassTag[Decl]) extends Ref[G, Decl] {
  // Sometimes Nothing ends up in Decl, which is never useful, so we try to crash a bit earlier when that happens.
  require(tag != ClassTag.Nothing)

  // Occasionally useful for debugging: the stack trace where the LazyRef is created
  // private val debugTrace = Thread.currentThread().getStackTrace

  // Capture lazyDecl into a lambda, so that lazyDecl is not implicitly added as a field in the LazyRef class.
  private var computeDecl: () => Declaration[G] = () => lazyDecl

  // Make decl lazy, so its evaluation is delayed, but only performed once. It is acceptable that the `decl` is
  // recomputed when it crashes, e.g. when viewed from a debugging context.
  lazy val decl: Decl = {
    val result = computeDecl()
    // Clear out the value of computeDecl once we have computed the declaration. If we wouldn't do this, we would create
    // long chains of LazyRef(() => LazyRef(() => LazyRef(...).decl).decl).decl, which would be hard on the garbage
    // collector.
    computeDecl = null
    result match {
      case decl: /*tagged*/ Decl => decl
      case other => throw MistypedRef(other, tag)
    }
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: LazyRef[G, Decl] if eqMeasure.nonEmpty && other.eqMeasure.nonEmpty =>
      eqMeasure.get == other.eqMeasure.get
    case other => super.equals(other)
  }

  override def hashCode(): Int = eqMeasure.map(_.hashCode()).getOrElse(super.hashCode())
}
