package org.openzen.zenscript.codemodel.member;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.expression.CallArguments;
import org.openzen.zenscript.codemodel.expression.Expression;
import org.openzen.zenscript.codemodel.expression.UnaryExpression;
import org.openzen.zenscript.codemodel.scope.TypeScope;
import org.openzen.zenscript.codemodel.type.TypeID;

public class UnaryStaticOperatorMember implements ICallableMember {
	private final UnaryExpression.Operator operator;
	private final TypeID operandType;

	public UnaryStaticOperatorMember(UnaryExpression.Operator operator, TypeID operandType) {
		this.operator = operator;
		this.operandType = operandType;
	}

	@Override
	public Expression callVirtual(CodePosition position, TypeScope scope, Expression target, CallArguments arguments) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Expression callStatic(CodePosition position, TypeScope scope, CallArguments arguments) {
		return new UnaryExpression(
				position,
				arguments.arguments[0].castImplicit(position, scope, operandType),
				operator,
				typeRegistry);
	}
}
