package vct.main.stages

import vct.col.ast.Verification
import hre.io.{RWFile, Readable}
import vct.col.print.Printer
import vct.col.newveymont.VeyMontUtil
import vct.options.Options

import java.nio.file.Path
object SaveStage {
  def ofOptions(options : Options) : SaveStage = {
    val newFileName = VeyMontUtil.getAnnotatedFileName(options.veymontOutput.getFileName.toString)
    SaveStage(Path.of(options.veymontOutput.toAbsolutePath.getParent.toString,newFileName))
  }
}
case class SaveStage(path : Path) extends Stage[Verification[_],Seq[Readable]]{
  override def friendlyName: String = "SaveStage"

  override def progressWeight: Int = 5

  override def run(in: Verification[_]): Seq[Readable] = {
    RWFile(path.toFile).write(writer => Printer(writer).print(in))
    Seq(RWFile(path.toFile))
  }
}
