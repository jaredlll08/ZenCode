package org.openzen.zenscript.scripting.visitor;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.expression.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExpressionFindingExpressionVisitor implements ExpressionVisitor<Optional<Expression>> {
	private final CodePosition queriedPosition;
	private static final Logger LOG = Logger.getGlobal();

	public ExpressionFindingExpressionVisitor(CodePosition queriedPosition) {
		this.queriedPosition = queriedPosition;
	}

	@Override
	public Optional<Expression> visitAndAnd(AndAndExpression expression) {
		return checkExpressions(expression.left, expression.right);
	}

	@Override
	public Optional<Expression> visitArray(ArrayExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitCompare(CompareExpression expression) {
		return checkExpressions(expression.left, expression.right);
	}

	@Override
	public Optional<Expression> visitCall(CallExpression expression) {
		LOG.log(Level.INFO, "CALL", expression.position.toLongString());

		return checkExpressions(expression.arguments.arguments);
	}

	@Override
	public Optional<Expression> visitCallStatic(CallStaticExpression expression) {
		LOG.log(Level.INFO, "CALL_STATIC", expression.position.toLongString());
		return checkExpressions(expression.arguments.arguments);
	}

	@Override
	public Optional<Expression> visitCallSuper(CallSuperExpression expression) {

		return checkExpressions(expression.arguments.arguments);
	}

	@Override
	public Optional<Expression> visitCapturedClosure(CapturedClosureExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitCapturedLocalVariable(CapturedLocalVariableExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitCapturedParameter(CapturedParameterExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitCapturedThis(CapturedThisExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitCheckNull(CheckNullExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitCoalesce(CoalesceExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConditional(ConditionalExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantBool(ConstantBoolExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantByte(ConstantByteExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantChar(ConstantCharExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantDouble(ConstantDoubleExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantFloat(ConstantFloatExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantInt(ConstantIntExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantLong(ConstantLongExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantSByte(ConstantSByteExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantShort(ConstantShortExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantString(ConstantStringExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantUInt(ConstantUIntExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantULong(ConstantULongExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantUShort(ConstantUShortExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstantUSize(ConstantUSizeExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstructorThisCall(ConstructorThisCallExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitConstructorSuperCall(ConstructorSuperCallExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitEnumConstant(EnumConstantExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitFunction(FunctionExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitGetField(GetFieldExpression expression) {
		return expression.target.accept(this);
	}

	@Override
	public Optional<Expression> visitGetFunctionParameter(GetFunctionParameterExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitGetLocalVariable(GetLocalVariableExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitGetMatchingVariantField(GetMatchingVariantField expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitGetStaticField(GetStaticFieldExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitGlobal(GlobalExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitGlobalCall(GlobalCallExpression expression) {
		LOG.log(Level.INFO, "GLOBAL_CALL", expression.position.toLongString());
		return checkExpressions(expression.arguments.arguments);
	}

	@Override
	public Optional<Expression> visitInterfaceCast(InterfaceCastExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitIs(IsExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitMakeConst(MakeConstExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitMap(MapExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitMatch(MatchExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitNull(NullExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitOrOr(OrOrExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitPanic(PanicExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitPlatformSpecific(Expression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitPostCall(PostCallExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitRange(RangeExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitSameObject(SameObjectExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitSetField(SetFieldExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitSetFunctionParameter(SetFunctionParameterExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitSetLocalVariable(SetLocalVariableExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitSetStaticField(SetStaticFieldExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitSupertypeCast(SupertypeCastExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitSubtypeCast(SubtypeCastExpression expression) {
		return Optional.empty();
	}

	@Override
	public Optional<Expression> visitThis(ThisExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitThrow(ThrowExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitTryConvert(TryConvertExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitTryRethrowAsException(TryRethrowAsExceptionExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitTryRethrowAsResult(TryRethrowAsResultExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitVariantValue(VariantValueExpression expression) {
		return Optional.of(expression);
	}

	@Override
	public Optional<Expression> visitWrapOptional(WrapOptionalExpression expression) {
		return expression.value.accept(this);
	}

	private Optional<Expression> checkExpressions(Expression... expressions) {
		return Arrays.stream(expressions)
				.filter(expression -> expression.position.containsFully(queriedPosition))
				.map(expression -> expression.accept(this))
				.findAny()
				.flatMap(Function.identity());
	}
}
