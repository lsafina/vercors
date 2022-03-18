package vct.main.modes

import com.typesafe.scalalogging.LazyLogging
import vct.options.Options
import hre.io.Readable
import vct.col.origin.{BlameCollector, VerificationFailure}
import vct.main.stages.Stages
import vct.parsers.transform.ConstantBlameProvder
import vct.result.VerificationError

case object Verify extends LazyLogging {
  val EXIT_CODE_SUCCESS = 0
  val EXIT_CODE_FAILURE = 1
  val EXIT_CODE_ERROR = 2

  def verifyDefault(inputs: Seq[Readable]): Either[VerificationError, Seq[VerificationFailure]] = {
    val collector = BlameCollector()
    Stages.default(ConstantBlameProvder(collector)).run(inputs) match {
      case Left(error) => Left(error)
      case Right(()) => Right(collector.errs.toSeq)
    }
  }

  def verifyWithOptions(options: Options, inputs: Seq[Readable]): Either[VerificationError, Seq[VerificationFailure]] = {
    val collector = BlameCollector()
    Stages.ofOptions(options, ConstantBlameProvder(collector)).run(inputs) match {
      case Left(error) => Left(error)
      case Right(()) => Right(collector.errs.toSeq)
    }
  }

  def runOptions(options: Options): Int = {
    verifyWithOptions(options, options.inputs) match {
      case Left(err) =>
        logger.error(err.text)
        EXIT_CODE_ERROR
      case Right(Nil) =>
        logger.info("Verification completed successfully.")
        EXIT_CODE_SUCCESS
      case Right(fails) =>
        fails.foreach(fail => logger.error(fail.text))
        EXIT_CODE_FAILURE
    }
  }
}
