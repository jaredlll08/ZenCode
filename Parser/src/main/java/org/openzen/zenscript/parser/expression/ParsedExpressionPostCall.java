package org.openzen.zenscript.parser.expression;

import org.openzen.zenscript.codemodel.compilation.expression.AbstractCompilingExpression;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.OperatorType;
import org.openzen.zenscript.codemodel.compilation.*;
import org.openzen.zenscript.codemodel.expression.Expression;

public class ParsedExpressionPostCall extends ParsedExpression {
	private final CompilableExpression value;
	private final OperatorType operator;

	public ParsedExpressionPostCall(CodePosition position, CompilableExpression value, OperatorType operator) {
		super(position);

		this.value = value;
		this.operator = operator;
	}

	@Override
	public CompilingExpression compile(ExpressionCompiler compiler) {
		return new Compiling(compiler, position, value.compile(compiler), operator);
	}

	private static class Compiling extends AbstractCompilingExpression {
		private final CompilingExpression value;
		private final OperatorType operator;

		public Compiling(ExpressionCompiler compiler, CodePosition position, CompilingExpression value, OperatorType operator) {
			super(compiler, position);

			this.value = value;
			this.operator = operator;
		}

		@Override
		public Expression eval() {
			Expression value = this.value.eval();
			return compiler.resolve(value.type).findOperator(operator)
					.map(operator -> operator.callPostfix(compiler.at(position), value))
					.orElseGet(() -> compiler.at(position).invalid(CompileErrors.noOperatorInType(value.type, operator)));
		}

		@Override
		public CastedExpression cast(CastedEval cast) {
			return cast.of(eval());
		}
	}
}
