package vct.main

import vct.col.ast.{CheckError, DiagnosticOrigin, IncomparableTypes, OutOfScopeError, Program, TypeError, TypeErrorText}
import vct.col.newrewrite.JavaSpecificToCol
import vct.col.resolve.{ResolveReferences, ResolveTypes}
import vct.parsers.Parsers
import vct.result.VerificationResult
import vct.result.VerificationResult.{SystemError, UserError}
import vct.test.CommandLineTesting

import java.nio.file.Path
import scala.jdk.CollectionConverters._

case object Test {
  var files = 0
  var errors = 0
  var crashes = 0

  def main(args: Array[String]): Unit = {
    try {
      CommandLineTesting.getCases.values.filter(_.tools.contains("silicon")).foreach(c => {
        c.files.asScala.filter(f =>
          f.toString.endsWith(".java") ||
            f.toString.endsWith(".c") ||
            f.toString.endsWith(".pvl")).foreach(tryParse)
      })

//      tryParse(Path.of("examples/carp/forward-host.pvl"))
    } finally {
      println(s"Out of $files files, $errors threw a SystemError and $crashes crashed.")
    }
  }

  def printErrorsOr(errors: Seq[CheckError])(otherwise: => Unit): Unit = {
    if(errors.isEmpty) otherwise
    else errors.foreach {
      case TypeError(expr, expectedType) =>
        println(expr.o.messageInContext(s"Expected to be of type $expectedType, but got ${expr.t}"))
      case TypeErrorText(expr, message) =>
        println(expr.o.messageInContext(message(expr.t)))
      case OutOfScopeError(ref) =>
        ref.decl.o.messageInContext("Out of scope!")
      case IncomparableTypes(left, right) =>
        println(s"Types $left and $right are incomparable")
    }
  }

  def tryParse(path: Path): Unit = try {
    files += 1
    println(path)
    var program = Program(Parsers.parse(path))(DiagnosticOrigin)
    val extraDecls = ResolveTypes.resolve(program)
    program = Program(program.declarations ++ extraDecls)(DiagnosticOrigin)
    val errors = ResolveReferences.resolve(program)
    printErrorsOr(errors) {
      program = JavaSpecificToCol().dispatch(program)
      printErrorsOr(program.check){}
    }
  } catch {
    case err: SystemError =>
      println(err.text)
      errors += 1
    case res: VerificationResult =>
      println(res.text)
    case e: Throwable =>
      e.printStackTrace()
      crashes += 1
  }
}