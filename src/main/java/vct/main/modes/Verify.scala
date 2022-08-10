package vct.main.modes

import com.typesafe.scalalogging.LazyLogging
import vct.options.Options
import hre.io.Readable
import vct.col.origin.{BlameCollector, TableEntry, VerificationFailure}
import vct.main.Main.{EXIT_CODE_ERROR, EXIT_CODE_SUCCESS, EXIT_CODE_VERIFICATION_FAILURE}
import vct.main.stages.{Backend, ExpectedErrors, Parsing, Resolution, SilverBackend, SilverTransformation, Stages, Transformation}
import vct.parsers.transform.{BlameProvider, ConstantBlameProvider}
import vct.result.VerificationError
import viper.api.{Carbon, Silicon}

case object Verify extends LazyLogging {
  def verifyWithSilicon(inputs: Seq[Readable]): Either[VerificationError, Seq[VerificationFailure]] = {
    val collector = BlameCollector()
    val stages = silicon(ConstantBlameProvider(collector))
    logger.debug(stages.toString)
    stages.run(inputs) match {
      case Left(error) => Left(error)
      case Right(()) => Right(collector.errs.toSeq)
    }
  }

  def verifyWithCarbon(inputs: Seq[Readable]): Either[VerificationError, Seq[VerificationFailure]] = {
    val collector = BlameCollector()
    val stages = carbon(ConstantBlameProvider(collector))
    logger.debug(stages.toString)
    stages.run(inputs) match {
      case Left(error) => Left(error)
      case Right(()) => Right(collector.errs.toSeq)
    }
  }

  def verifyWithOptions(options: Options, inputs: Seq[Readable]): Either[VerificationError, Seq[VerificationFailure]] = {
    val collector = BlameCollector()
    val stages = ofOptions(options, ConstantBlameProvider(collector))
    logger.debug(stages.toString)
    stages.run(inputs) match {
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
        if(fails.size <= 2) fails.foreach(fail => logger.error(fail.desc))
        else logger.error(TableEntry.render(fails.map(_.asTableEntry)))
        EXIT_CODE_VERIFICATION_FAILURE
    }
  }



  def ofOptions(options: Options, blameProvider: BlameProvider): Stages[Seq[Readable], Unit] = {
    Parsing.ofOptions(options, blameProvider)
      .thenRun(Resolution.ofOptions(options, blameProvider))
      .thenRun(Transformation.ofOptions(options))
      .thenRun(Backend.ofOptions(options))
      .thenRun(ExpectedErrors.ofOptions(options))
  }

  def silicon(blameProvider: BlameProvider): Stages[Seq[Readable], Unit] = {
    Parsing(blameProvider)
      .thenRun(Resolution(blameProvider))
      .thenRun(SilverTransformation())
      .thenRun(SilverBackend(Silicon()))
      .thenRun(ExpectedErrors())
  }

  def carbon(blameProvider: BlameProvider): Stages[Seq[Readable], Unit] = {
    Parsing(blameProvider)
      .thenRun(Resolution(blameProvider))
      .thenRun(SilverTransformation())
      .thenRun(SilverBackend(Carbon()))
      .thenRun(ExpectedErrors())
  }
}
