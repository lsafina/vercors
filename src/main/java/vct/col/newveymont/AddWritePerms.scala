package vct.col.newveymont

import vct.col.rewrite.{Generation, Rewriter, RewriterBuilder}

case object AddWritePerms extends RewriterBuilder {
  override def key: String = "addWritePerms"

  override def desc: String = "Add write permsissions for all references used in a class"
}

case class AddWritePerms [Pre <: Generation]() extends Rewriter[Pre] {
}
