package org.openzen.zenscript.semantics.visitor;

import org.openzen.zenscript.codemodel.expression.*;
import org.openzen.zenscript.codemodel.statement.*;
import org.openzen.zenscript.semantics.SemanticToken;
import org.openzen.zenscript.semantics.SemanticTokenType;

import java.util.*;

public class SemanticTokenVisitor implements StatementVisitor<Void>, ExpressionVisitor<Void> {

	public final List<SemanticToken> tokens = new ArrayList<>();

	private void check(Statement... statements) {
		if (statements != null) {
			Arrays.stream(statements)
					.filter(Objects::nonNull)
					.forEach(statement -> statement.accept(this));
		}
	}

	private void check(Expression... expressions) {
		if (expressions != null) {
			Arrays.stream(expressions)
					.filter(Objects::nonNull)
					.forEach(e -> e.accept(this));
		}
	}

	@Override
	public Void visitBlock(BlockStatement statement) {
		check(statement.statements);
		return null;
	}

	@Override
	public Void visitBreak(BreakStatement statement) {
		return null;
	}

	@Override
	public Void visitContinue(ContinueStatement statement) {
		return null;
	}

	@Override
	public Void visitDoWhile(DoWhileStatement statement) {
		statement.content.accept(this);
		return null;
	}

	@Override
	public Void visitEmpty(EmptyStatement statement) {
		return null;
	}

	@Override
	public Void visitExpression(ExpressionStatement statement) {
		statement.expression.accept(this);
		return null;
	}

	@Override
	public Void visitForeach(ForeachStatement statement) {
		check(statement.loopVariables);
		check(statement.getContent());
		return null;
	}

	@Override
	public Void visitIf(IfStatement statement) {
		check(statement.condition);
		check(statement.onThen);
		check(statement.onElse);
		return null;
	}

	@Override
	public Void visitLock(LockStatement statement) {
		check(statement.object);
		check(statement.content);
		return null;
	}

	@Override
	public Void visitReturn(ReturnStatement statement) {
		check(statement.value);
		return null;
	}

	@Override
	public Void visitSwitch(SwitchStatement statement) {
		check(statement.value);
		statement.cases.stream().map(switchCase -> switchCase.statements).forEach(this::check);
		return null;
	}

	@Override
	public Void visitThrow(ThrowStatement statement) {
		check(statement.value);
		return null;
	}

	@Override
	public Void visitTryCatch(TryCatchStatement statement) {
		check(statement.resource);
		check(statement.content);
		statement.catchClauses.forEach(catchClause -> {
			check(catchClause.exceptionVariable);
			check(catchClause.content);
		});
		check(statement.finallyClause);
		return null;
	}

	@Override
	public Void visitVar(VarStatement statement) {
		check(statement.initializer);
		// TODO check type
		tokens.add(new SemanticToken(statement.position, statement.name.length(), SemanticTokenType.COMMENT));
		return null;
	}

	@Override
	public Void visitWhile(WhileStatement statement) {
		check(statement.condition);
		check(statement.content);
		return null;
	}

	// ====================================


	@Override
	public Void visitAndAnd(AndAndExpression expression) {
		return null;
	}

	@Override
	public Void visitArray(ArrayExpression expression) {
		return null;
	}

	@Override
	public Void visitCompare(CompareExpression expression) {
		return null;
	}

	@Override
	public Void visitCall(CallExpression expression) {
		return null;
	}

	@Override
	public Void visitCallStatic(CallStaticExpression expression) {
		return null;
	}

	@Override
	public Void visitCallSuper(CallSuperExpression expression) {
		return null;
	}

	@Override
	public Void visitCapturedClosure(CapturedClosureExpression expression) {
		return null;
	}

	@Override
	public Void visitCapturedLocalVariable(CapturedLocalVariableExpression expression) {
		return null;
	}

	@Override
	public Void visitCapturedParameter(CapturedParameterExpression expression) {
		return null;
	}

	@Override
	public Void visitCapturedThis(CapturedThisExpression expression) {
		return null;
	}

	@Override
	public Void visitCheckNull(CheckNullExpression expression) {
		return null;
	}

	@Override
	public Void visitCoalesce(CoalesceExpression expression) {
		return null;
	}

	@Override
	public Void visitConditional(ConditionalExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantBool(ConstantBoolExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantByte(ConstantByteExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantChar(ConstantCharExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantDouble(ConstantDoubleExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantFloat(ConstantFloatExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantInt(ConstantIntExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantLong(ConstantLongExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantSByte(ConstantSByteExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantShort(ConstantShortExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantString(ConstantStringExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantUInt(ConstantUIntExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantULong(ConstantULongExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantUShort(ConstantUShortExpression expression) {
		return null;
	}

	@Override
	public Void visitConstantUSize(ConstantUSizeExpression expression) {
		return null;
	}

	@Override
	public Void visitConstructorThisCall(ConstructorThisCallExpression expression) {
		return null;
	}

	@Override
	public Void visitConstructorSuperCall(ConstructorSuperCallExpression expression) {
		return null;
	}

	@Override
	public Void visitEnumConstant(EnumConstantExpression expression) {
		return null;
	}

	@Override
	public Void visitFunction(FunctionExpression expression) {
		return null;
	}

	@Override
	public Void visitGetField(GetFieldExpression expression) {
		return null;
	}

	@Override
	public Void visitGetFunctionParameter(GetFunctionParameterExpression expression) {
		return null;
	}

	@Override
	public Void visitGetLocalVariable(GetLocalVariableExpression expression) {
		return null;
	}

	@Override
	public Void visitGetMatchingVariantField(GetMatchingVariantField expression) {
		return null;
	}

	@Override
	public Void visitGetStaticField(GetStaticFieldExpression expression) {
		return null;
	}

	@Override
	public Void visitGlobal(GlobalExpression expression) {
		return null;
	}

	@Override
	public Void visitGlobalCall(GlobalCallExpression expression) {
		return null;
	}

	@Override
	public Void visitInterfaceCast(InterfaceCastExpression expression) {
		return null;
	}

	@Override
	public Void visitIs(IsExpression expression) {
		return null;
	}

	@Override
	public Void visitMakeConst(MakeConstExpression expression) {
		return null;
	}

	@Override
	public Void visitMap(MapExpression expression) {
		return null;
	}

	@Override
	public Void visitMatch(MatchExpression expression) {
		return null;
	}

	@Override
	public Void visitNull(NullExpression expression) {
		return null;
	}

	@Override
	public Void visitOrOr(OrOrExpression expression) {
		return null;
	}

	@Override
	public Void visitPanic(PanicExpression expression) {
		return null;
	}

	@Override
	public Void visitPlatformSpecific(Expression expression) {
		return null;
	}

	@Override
	public Void visitPostCall(PostCallExpression expression) {
		return null;
	}

	@Override
	public Void visitRange(RangeExpression expression) {
		return null;
	}

	@Override
	public Void visitSameObject(SameObjectExpression expression) {
		return null;
	}

	@Override
	public Void visitSetField(SetFieldExpression expression) {
		return null;
	}

	@Override
	public Void visitSetFunctionParameter(SetFunctionParameterExpression expression) {
		return null;
	}

	@Override
	public Void visitSetLocalVariable(SetLocalVariableExpression expression) {
		return null;
	}

	@Override
	public Void visitSetStaticField(SetStaticFieldExpression expression) {
		return null;
	}

	@Override
	public Void visitSupertypeCast(SupertypeCastExpression expression) {
		return null;
	}

	@Override
	public Void visitSubtypeCast(SubtypeCastExpression expression) {
		return null;
	}

	@Override
	public Void visitThis(ThisExpression expression) {
		return null;
	}

	@Override
	public Void visitThrow(ThrowExpression expression) {
		return null;
	}

	@Override
	public Void visitTryConvert(TryConvertExpression expression) {
		return null;
	}

	@Override
	public Void visitTryRethrowAsException(TryRethrowAsExceptionExpression expression) {
		return null;
	}

	@Override
	public Void visitTryRethrowAsResult(TryRethrowAsResultExpression expression) {
		return null;
	}

	@Override
	public Void visitVariantValue(VariantValueExpression expression) {
		return null;
	}

	@Override
	public Void visitWrapOptional(WrapOptionalExpression expression) {
		return null;
	}
}
