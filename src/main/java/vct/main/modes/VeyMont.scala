package vct.main.modes

import hre.io
import hre.io.Readable
import vct.col.ast.Verification
import vct.col.origin.{BlameCollector, TableEntry, VerificationFailure}
import vct.col.rewrite.Generation
import vct.main.Main.{EXIT_CODE_ERROR, EXIT_CODE_SUCCESS, EXIT_CODE_VERIFICATION_FAILURE}
import vct.main.modes.Verify.logger
import vct.main.stages.{ExpectedErrors, Parsing, Resolution, SaveStage, Stages, Transformation}
import vct.options.{Options, PathOrStd}
import vct.parsers.transform.{BlameProvider, ConstantBlameProvider}
import vct.result.VerificationError

object VeyMont {
  def runOptions(options: Options): Int = {
    val collector = BlameCollector()
    val stagesPre = getVeyMontStages(options, ConstantBlameProvider(collector))
    runStages(stagesPre,options.inputs,collector)
 /*   val verifyOptions = getVerifyOptions(options)
    Verify.runOptions(verifyOptions)
    val stagesPost = getVeyMontStages(options,ConstantBlameProvider(collector))
    val veymontPostOptions = getVeyMontPostOptions(verifyOptions)
    runStages(stagesPost,veymontPostOptions.inputs,collector) */
  }

  def getVeyMontStages(options: Options, blameProvider: ConstantBlameProvider) = {
    Parsing.ofOptions(options, blameProvider)
      .thenRun(Resolution.ofOptions(options, blameProvider))
      .thenRun(Transformation.veymontOfOptions(options))
      .thenRun(SaveStage.ofOptions(options))
  }

  /*
  def getVeyMontStages(options: Options, blameProvider: BlameProvider) = {
    val verifyOptions = getVerifyOptions(options)
    Parsing.ofOptions(options, blameProvider)
      .thenRun(Resolution.ofOptions(options, blameProvider))
      .thenRun(Transformation.ofOptions(options)) //returned Seq[RWFile] met glob file
      .thenRun(SaveStage) //return ContexStage
      .thenRun(input => (input, input) )
      .thenRun(WithoutContext(Verify.ofOptions(verifyOptions,blameProvider)))
      .thenRun { case (_, input) => input }
      .thenRun(Transformation.ofOptions())
      //.thenRun(ExpectedErrors.ofOptions(options))
  }
*/
  def getVerifyOptions(options : Options) = {
    options
  }

  def getVeyMontPostOptions(options: Options) = {
    options
  }

  def runStages(stages: Stages[Seq[Readable], _], inputs : Seq[PathOrStd], collector: BlameCollector) : Int= {
    //logger.debug(stages.toString)
    stages.run(inputs) match {
      case Left(err) =>
      //  logger.error(err.text)
        val tmp = err
        EXIT_CODE_ERROR
      case Right(_) =>
      //  logger.info("Verification completed successfully.")
        EXIT_CODE_SUCCESS
    }
  }
}
