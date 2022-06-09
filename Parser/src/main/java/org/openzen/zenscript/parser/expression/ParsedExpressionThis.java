package org.openzen.zenscript.parser.expression;

import org.openzen.zenscript.codemodel.compilation.expression.AbstractCompilingExpression;
import org.openzen.zenscript.codemodel.compilation.CompileErrors;
import org.openzen.zenscript.codemodel.compilation.*;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.expression.Expression;

import java.util.Optional;

public class ParsedExpressionThis extends ParsedExpression {
	public ParsedExpressionThis(CodePosition position) {
		super(position);
	}

	@Override
	public CompilingExpression compile(ExpressionCompiler compiler) {
		return compiler.getLocalType()
				.<CompilingExpression>map(t -> new Compiling(compiler, position, t))
				.orElseGet(() -> compiler.invalid(position, CompileErrors.noThisInScope()));
	}

	private static class Compiling extends AbstractCompilingExpression {
		private final LocalType type;

		public Compiling(ExpressionCompiler compiler, CodePosition position, LocalType type) {
			super(compiler, position);
			this.type = type;
		}

		@Override
		public Expression eval() {
			return compiler.at(position).getThis(this.type.getThisType());
		}

		@Override
		public CastedExpression cast(CastedEval cast) {
			return cast.of(eval());
		}

		@Override
		public Optional<StaticCallable> call() {
			return Optional.of(type.thisCall());
		}
	}
}
