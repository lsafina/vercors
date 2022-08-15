package vct.col.newveymont

object VeyMontUtil {

  private val veymontAnnotatedSuffix = "-glob.pvl"

  def getAnnotatedFileName(inputFileName : String) : String = inputFileName.substring(0,inputFileName.length-4) + veymontAnnotatedSuffix

}
