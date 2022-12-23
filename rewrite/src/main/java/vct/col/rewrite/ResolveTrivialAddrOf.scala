package vct.col.rewrite
import vct.col.ast.{AddrOf, DerefPointer, Expr, PointerAdd, PointerSubscript}

case object ResolveTrivialAddrOf extends RewriterBuilder {
  override def key: String = "addrOf"
  override def desc: String = "Desugar trivial instances of the address-of operator (&a[i] and &*p)"
}

case class ResolveTrivialAddrOf[Pre <: Generation]() extends Rewriter[Pre] {
  override def dispatch(e: Expr[Pre]): Expr[Rewritten[Pre]] = e match {
    case AddrOf(DerefPointer(p)) => dispatch(p)
    case AddrOf(sub @ PointerSubscript(p, i)) => PointerAdd(dispatch(p), dispatch(i))(sub.blame)(e.o)
    case other => rewriteDefault(other)
  }
}
