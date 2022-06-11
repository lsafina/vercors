package vct.col.newrewrite.lang

import com.typesafe.scalalogging.LazyLogging
import vct.col.ast.{PVLInvocation, _}
import vct.col.origin.{Origin, PanicBlame, PostBlameSplit, SourceNameOrigin, TrueSatisfiable}
import vct.col.rewrite.{Generation, Rewritten}
import vct.col.util.AstBuildHelpers._
import vct.col.ast.RewriteHelpers._
import vct.col.ast.stmt.decl.Method
import vct.col.newrewrite.lang.LangPVLToCol.PVLSourceNameOrigin
import vct.col.newrewrite.lang.LangSpecificToCol.{NotAValue, ThisVar}
import vct.col.ref.Ref
import vct.col.resolve.{BuiltinField, BuiltinInstanceMethod, ImplicitDefaultPVLConstructor, RefADTFunction, RefAxiomaticDataType, RefClass, RefField, RefFunction, RefInstanceFunction, RefInstanceMethod, RefInstancePredicate, RefModel, RefModelAction, RefModelField, RefModelProcess, RefPVLConstructor, RefPredicate, RefProcedure, RefVariable}
import vct.col.util.{AstBuildHelpers, SuccessionMap}

case object LangPVLToCol {
  case class PVLSourceNameOrigin(qualifiedName: String, o: Origin) extends Origin {
    override def preferredName: String = o.preferredName
    override def context: String = o.context
    override def inlineContext: String = o.inlineContext
    override def shortPosition: String = o.shortPosition
  }
}

case class LangPVLToCol[Pre <: Generation](rw: LangSpecificToCol[Pre]) extends LazyLogging {
  type Post = Rewritten[Pre]
  implicit val implicitRewriter: AbstractRewriter[Pre, Post] = rw

  val pvlDefaultConstructor: SuccessionMap[Class[Pre], Procedure[Post]] = SuccessionMap()

  def rewriteConstructor(cons: PVLConstructor[Pre]): Unit = {
    implicit val o: Origin = cons.o
    val t = TClass[Post](rw.succ(rw.currentClass.top))
    val resVar = new Variable(t)
    withResult((result: Result[Post]) => new Procedure[Post](
      returnType = t,
      args = rw.collectInScope(rw.variableScopes) { cons.args.foreach(rw.dispatch) },
      outArgs = Nil,
      typeArgs = Nil,
      body = rw.currentThis.having(resVar.get) { cons.body.map(body => Scope(Seq(resVar), Block(Seq(
        assignLocal(resVar.get, NewObject[Post](rw.succ(rw.currentClass.top))),
        rw.dispatch(body),
        Commit(resVar.get)(cons.blame),
        Return(resVar.get),
      )))) },
      contract = rw.currentThis.having(result) { cons.contract.rewrite(
        ensures = SplitAccountedPredicate(
          left = UnitAccountedPredicate((result !== Null()) && (TypeOf(result) === TypeValue(t))),
          right = rw.dispatch(cons.contract.ensures),
        )
      ) },
      focus = cons.focus, ignore = cons.ignore
    )(PostBlameSplit.left(PanicBlame("Constructor cannot return null value or value of wrong type."), cons.blame)
    )(PVLSourceNameOrigin(s"${rw.currentClass.top.o.preferredName}.constructor", cons.o))).succeedDefault(cons)
  }

  def maybeDeclareDefaultConstructor(cls: Class[Pre]): Unit = {
    if (cls.declarations.collectFirst { case _: PVLConstructor[Pre] => () }.isEmpty) {
      implicit val o: Origin = cls.o
      val t = TClass[Post](rw.succ(cls))
      val resVar = new Variable[Post](t)
      val res = Local[Post](resVar.ref)(ThisVar)
      val defaultBlame = PanicBlame("The postcondition of a default constructor cannot fail (but what about commit?).")

      pvlDefaultConstructor(cls) = withResult((result: Result[Post]) => new Procedure(
        t,
        Nil, Nil, Nil,
        Some(Scope(Seq(resVar), Block(Seq(
          assignLocal(res, NewObject[Post](rw.succ(cls))),
          Commit(res)(defaultBlame),
          Return(res),
        )))),
        ApplicableContract(
          UnitAccountedPredicate(tt),
          UnitAccountedPredicate(AstBuildHelpers.foldStar(cls.declarations.collect {
            case field: InstanceField[Pre] =>
              fieldPerm[Post](result, rw.succ(field), WritePerm())
          })), tt, Nil, Nil, Nil, None,
        )(TrueSatisfiable)
        // No "SourceNameOrigin" because this is an implitily generated constructor, so it doesn't get an explicit name/origin
        // This ensures it doesn't show up later in summaries, e.g. in FilterAndAbstractDeclarations
      )(defaultBlame)).declareDefault(rw)
    }
  }

  def local(local: PVLLocal[Pre]): Expr[Post] = {
    implicit val o: Origin = local.o

    local.ref.get match {
      case RefAxiomaticDataType(decl) => throw NotAValue(local)
      case RefVariable(decl) => Local(rw.succ(decl))
      case RefModelField(decl) => ModelDeref[Post](rw.currentThis.top, rw.succ(decl))(local.blame)
      case RefClass(decl) => throw NotAValue(local)
      case RefField(decl) => Deref[Post](rw.currentThis.top, rw.succ(decl))(local.blame)
    }
  }

  def deref(deref: PVLDeref[Pre]): Expr[Post] = {
    implicit val o: Origin = deref.o
    deref.ref.get match {
      case RefModelField(decl) => ModelDeref[Post](rw.dispatch(deref.obj), rw.succ(decl))(deref.blame)
      case BuiltinField(f) => rw.dispatch(f(deref.obj))
      case RefField(decl) => Deref[Post](rw.dispatch(deref.obj), rw.succ(decl))(deref.blame)
    }
  }

  def invocation(inv: PVLInvocation[Pre]): Expr[Post] = {
    val PVLInvocation(obj, _, args, typeArgs, givenMap, yields) = inv
    implicit val o: Origin = inv.o

    inv.ref.get match {
      case RefFunction(decl) =>
        FunctionInvocation[Post](rw.succ(decl), args.map(rw.dispatch), typeArgs.map(rw.dispatch),
          givenMap.map { case (Ref(v), e) => (rw.succ(v), rw.dispatch(e)) },
          yields.map { case (Ref(e), Ref(v)) => (rw.succ(e), rw.succ(v)) })(inv.blame)
      case RefProcedure(decl) =>
        ProcedureInvocation[Post](rw.succ(decl), args.map(rw.dispatch), Nil, typeArgs.map(rw.dispatch),
          givenMap.map { case (Ref(v), e) => (rw.succ(v), rw.dispatch(e)) },
          yields.map { case (Ref(e), Ref(v)) => (rw.succ(e), rw.succ(v)) })(inv.blame)
      case RefPredicate(decl) =>
        PredicateApply[Post](rw.succ(decl), args.map(rw.dispatch), WritePerm())
      case RefInstanceFunction(decl) =>
        InstanceFunctionInvocation[Post](
          obj.map(rw.dispatch).getOrElse(rw.currentThis.top),
          rw.succ(decl),
          args.map(rw.dispatch),
          typeArgs.map(rw.dispatch),
          givenMap.map { case (Ref(v), e) => (rw.succ(v), rw.dispatch(e)) },
          yields.map { case (Ref(e), Ref(v)) => (rw.succ(e), rw.succ(v)) },
        )(inv.blame)
      case RefInstanceMethod(decl) =>
        MethodInvocation[Post](obj.map(rw.dispatch).getOrElse(rw.currentThis.top), rw.succ(decl), args.map(rw.dispatch), Nil, typeArgs.map(rw.dispatch),
          givenMap.map { case (Ref(v), e) => (rw.succ(v), rw.dispatch(e)) },
          yields.map { case (Ref(e), Ref(v)) => (rw.succ(e), rw.succ(v)) })(inv.blame)
      case RefInstancePredicate(decl) =>
        InstancePredicateApply[Post](obj.map(rw.dispatch).getOrElse(rw.currentThis.top), rw.succ(decl), args.map(rw.dispatch), WritePerm())
      case RefADTFunction(decl) =>
        ADTFunctionInvocation[Post](None, rw.succ(decl), args.map(rw.dispatch))
      case RefModelProcess(decl) =>
        ProcessApply[Post](rw.succ(decl), args.map(rw.dispatch))
      case RefModelAction(decl) =>
        ActionApply[Post](rw.succ(decl), args.map(rw.dispatch))
      case BuiltinInstanceMethod(f) =>
        rw.dispatch(f(obj.get)(args))
    }
  }

  def newClass(inv: PVLNew[Pre]): Expr[Post] = {
    val PVLNew(t, args, givenMap, yields) = inv
    implicit val o: Origin = inv.o
    inv.ref.get match {
      case RefModel(decl) => ModelNew[Post](rw.succ(decl))
      case RefPVLConstructor(decl) =>
        ProcedureInvocation[Post](rw.succ(decl), args.map(rw.dispatch), Nil, Nil,
          givenMap.map { case (Ref(v), e) => (rw.succ(v), rw.dispatch(e)) },
          yields.map { case (Ref(e), Ref(v)) => (rw.succ(e), rw.succ(v)) })(inv.blame)
      case ImplicitDefaultPVLConstructor() =>
        ProcedureInvocation[Post](pvlDefaultConstructor.ref(t.asInstanceOf[TClass[Pre]].cls.decl), args.map(rw.dispatch), Nil, Nil,
          givenMap.map { case (Ref(v), e) => (rw.succ(v), rw.dispatch(e)) },
          yields.map { case (Ref(e), Ref(v)) => (rw.succ(e), rw.succ(v)) })(inv.blame)
    }
  }

  def rewriteMethod(m: InstanceMethod[Pre]): Unit = {
    m.rewrite(o = PVLSourceNameOrigin(s"${rw.currentClass.top.o.preferredName}.${m.o.preferredName}", m.o)).succeedDefault(m)
  }

}
