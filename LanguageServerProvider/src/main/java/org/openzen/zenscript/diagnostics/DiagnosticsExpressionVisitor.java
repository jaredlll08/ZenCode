package org.openzen.zenscript.diagnostics;

import org.eclipse.lsp4j.Diagnostic;
import org.openzen.zenscript.OpenFileInfo;
import org.openzen.zenscript.codemodel.expression.*;
import org.openzen.zenscript.codemodel.statement.Statement;
import org.openzen.zenscript.codemodel.type.TypeID;

import java.util.List;

public class DiagnosticsExpressionVisitor implements ExpressionVisitor<Void> {

	private final List<Diagnostic> diagnostics;
	private DiagnosticsStatementVisitor statementVisitor;
	private DiagnosticsTypeVisitor typeVisitor;

	public DiagnosticsExpressionVisitor(List<Diagnostic> diagnostics) {
		this.diagnostics = diagnostics;
	}

	private void statement(Statement... statement) {
		if (this.statementVisitor == null) {
			this.statementVisitor = new DiagnosticsStatementVisitor(this.diagnostics);
		}
		if (statement != null) {
			for (Statement statement1 : statement) {
				if (statement1 != null) {
					statement1.accept(statementVisitor);
				}
			}
		}
	}

	private void expression(Expression... expression) {
		if (expression != null) {
			for (Expression expression1 : expression) {
				if (expression1 != null) {
					expression1.accept(this);
				}
			}
		}
	}

	private void type(TypeID... type) {
		if (this.typeVisitor == null) {
			this.typeVisitor = new DiagnosticsTypeVisitor(this.diagnostics);
		}
		if (type != null) {
			for (TypeID typeID : type) {
				if (typeID != null) {
					typeID.accept(typeVisitor);
				}
			}
		}
	}

	private void lambdaClosure(LambdaClosure closure) {
		expression(closure.captures.toArray(Expression.NONE));
	}

	private void baseExpression(Expression expression) {
		type(expression.type, expression.thrownType);
	}

	@Override
	public Void visitAndAnd(AndAndExpression expression) {
		baseExpression(expression);
		expression(expression.left, expression.right);
		return null;
	}

	@Override
	public Void visitArray(ArrayExpression expression) {
		baseExpression(expression);
		expression(expression.expressions);
		type(expression.arrayType);
		return null;
	}

	@Override
	public Void visitCompare(CompareExpression expression) {
		baseExpression(expression);
		expression(expression.left, expression.right);
		return null;
	}

	@Override
	public Void visitCall(CallExpression expression) {
		baseExpression(expression);
		expression(expression.target);
		return null;
	}

	@Override
	public Void visitCallStatic(CallStaticExpression expression) {
		baseExpression(expression);
		type(expression.target);
		return null;
	}

	@Override
	public Void visitCallSuper(CallSuperExpression expression) {
		baseExpression(expression);
		expression(expression.target);
		return null;
	}

	@Override
	public Void visitCapturedClosure(CapturedClosureExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		lambdaClosure(expression.closure);
		return null;
	}

	@Override
	public Void visitCapturedLocalVariable(CapturedLocalVariableExpression expression) {
		baseExpression(expression);
		lambdaClosure(expression.closure);
		//TODO validate
		return null;
	}

	@Override
	public Void visitCapturedParameter(CapturedParameterExpression expression) {
		baseExpression(expression);
		lambdaClosure(expression.closure);
		//TODO validate
		return null;
	}

	@Override
	public Void visitCapturedThis(CapturedThisExpression expression) {
		baseExpression(expression);
		lambdaClosure(expression.closure);
		return null;
	}

	@Override
	public Void visitCheckNull(CheckNullExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		return null;
	}

	@Override
	public Void visitCoalesce(CoalesceExpression expression) {
		baseExpression(expression);
		expression(expression.left, expression.right);
		return null;
	}

	@Override
	public Void visitConditional(ConditionalExpression expression) {
		baseExpression(expression);
		expression(expression.condition, expression.ifThen, expression.ifElse);
		return null;
	}

	@Override
	public Void visitConstantBool(ConstantBoolExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstantByte(ConstantByteExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstantChar(ConstantCharExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstantDouble(ConstantDoubleExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstantFloat(ConstantFloatExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstantInt(ConstantIntExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstantLong(ConstantLongExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstantSByte(ConstantSByteExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstantShort(ConstantShortExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstantString(ConstantStringExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstantUInt(ConstantUIntExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstantULong(ConstantULongExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstantUShort(ConstantUShortExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstantUSize(ConstantUSizeExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitConstructorThisCall(ConstructorThisCallExpression expression) {
		baseExpression(expression);
		type(expression.objectType);
		return null;
	}

	@Override
	public Void visitConstructorSuperCall(ConstructorSuperCallExpression expression) {
		baseExpression(expression);
		type(expression.objectType);
		return null;
	}

	@Override
	public Void visitEnumConstant(EnumConstantExpression expression) {
		baseExpression(expression);
		//TODO validate
		return null;
	}

	@Override
	public Void visitFunction(FunctionExpression expression) {
		baseExpression(expression);
		lambdaClosure(expression.closure);
		statement(expression.body);
		//TODO validate
		return null;
	}

	@Override
	public Void visitGetField(GetFieldExpression expression) {
		baseExpression(expression);
		expression(expression.target);
		return null;
	}

	@Override
	public Void visitGetFunctionParameter(GetFunctionParameterExpression expression) {
		baseExpression(expression);
		//TODO validate
		return null;
	}

	@Override
	public Void visitGetLocalVariable(GetLocalVariableExpression expression) {
		baseExpression(expression);
		//TODO validate
		return null;
	}

	@Override
	public Void visitGetMatchingVariantField(GetMatchingVariantField expression) {
		baseExpression(expression);
		//TODO validate
		return null;
	}

	@Override
	public Void visitGetStaticField(GetStaticFieldExpression expression) {
		baseExpression(expression);
		//TODO validate
		return null;
	}

	@Override
	public Void visitGlobal(GlobalExpression expression) {
		baseExpression(expression);
		expression(expression.resolution);
		return null;
	}

	@Override
	public Void visitGlobalCall(GlobalCallExpression expression) {
		baseExpression(expression);
		expression(expression.resolution);
		//TODO validate
		return null;
	}

	@Override
	public Void visitInterfaceCast(InterfaceCastExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		//TODO validate
		return null;
	}

	@Override
	public Void visitIs(IsExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		type(expression.isType);
		return null;
	}

	@Override
	public Void visitMakeConst(MakeConstExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		return null;
	}

	@Override
	public Void visitMap(MapExpression expression) {
		baseExpression(expression);
		expression(expression.keys);
		expression(expression.values);
		return null;
	}

	@Override
	public Void visitMatch(MatchExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		//TODO cases
		return null;
	}

	@Override
	public Void visitNull(NullExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitOrOr(OrOrExpression expression) {
		baseExpression(expression);
		expression(expression.left, expression.right);
		return null;
	}

	@Override
	public Void visitPanic(PanicExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		return null;
	}

	@Override
	public Void visitPlatformSpecific(Expression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitPostCall(PostCallExpression expression) {
		baseExpression(expression);
		expression(expression.target);
		return null;
	}

	@Override
	public Void visitRange(RangeExpression expression) {
		baseExpression(expression);
		expression(expression.from, expression.to);
		return null;
	}

	@Override
	public Void visitSameObject(SameObjectExpression expression) {
		baseExpression(expression);
		expression(expression.left, expression.right);
		return null;
	}

	@Override
	public Void visitSetField(SetFieldExpression expression) {
		baseExpression(expression);
		expression(expression.target);
		expression(expression.value);
		//TODO validate
		return null;
	}

	@Override
	public Void visitSetFunctionParameter(SetFunctionParameterExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		//TODO validate
		return null;
	}

	@Override
	public Void visitSetLocalVariable(SetLocalVariableExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		//TODO validate
		return null;
	}

	@Override
	public Void visitSetStaticField(SetStaticFieldExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		//TODO validate
		return null;
	}

	@Override
	public Void visitSupertypeCast(SupertypeCastExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		return null;
	}

	@Override
	public Void visitSubtypeCast(SubtypeCastExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		return null;
	}

	@Override
	public Void visitThis(ThisExpression expression) {
		baseExpression(expression);
		return null;
	}

	@Override
	public Void visitThrow(ThrowExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		return null;
	}

	@Override
	public Void visitTryConvert(TryConvertExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		return null;
	}

	@Override
	public Void visitTryRethrowAsException(TryRethrowAsExceptionExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		return null;
	}

	@Override
	public Void visitTryRethrowAsResult(TryRethrowAsResultExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		return null;
	}

	@Override
	public Void visitVariantValue(VariantValueExpression expression) {
		baseExpression(expression);
		expression(expression.arguments);
		//TODO validate
		return null;
	}

	@Override
	public Void visitWrapOptional(WrapOptionalExpression expression) {
		baseExpression(expression);
		expression(expression.value);
		return null;
	}

	@Override
	public Void visitInvalid(InvalidExpression expression) {
		baseExpression(expression);
		diagnostics.add(new Diagnostic(OpenFileInfo.codePositionToRange(expression.position), expression.error.description));
		return null;
	}

	@Override
	public Void visitInvalidAssign(InvalidAssignExpression expression) {
		baseExpression(expression);
		diagnostics.add(new Diagnostic(OpenFileInfo.codePositionToRange(expression.position), expression.target.error.description));
		return null;
	}
}
