package org.openzen.zenscript.codemodel.compilation.expression;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.GenericName;
import org.openzen.zenscript.codemodel.OperatorType;
import org.openzen.zenscript.codemodel.compilation.*;
import org.openzen.zenscript.codemodel.expression.Expression;
import org.openzen.zenscript.codemodel.ssa.CodeBlockStatement;
import org.openzen.zenscript.codemodel.ssa.SSAVariableCollector;
import org.openzen.zenscript.codemodel.type.TypeID;

import java.util.Optional;

public class TypeCompilingExpression implements CompilingExpression {
	private final ExpressionCompiler compiler;
	private final CodePosition position;
	private final TypeID type;

	public TypeCompilingExpression(ExpressionCompiler compiler, CodePosition position, TypeID type) {
		this.compiler = compiler;
		this.position = position;
		this.type = type;
	}

	@Override
	public Expression eval() {
		return compiler.at(position).invalid(CompileErrors.cannotUseTypeAsValue());
	}

	@Override
	public CastedExpression cast(CastedEval cast) {
		return CastedExpression.invalid(eval());
	}

	@Override
	public boolean canConstructAs(TypeID type) {
		return false;
	}

	@Override
	public Optional<CompilingCallable> call() {
		return compiler.resolve(type).findStaticOperator(OperatorType.CALL)
				.map(operator -> new StaticCompilingCallable(compiler, operator));
	}

	@Override
	public CompilingExpression getMember(CodePosition position, GenericName name) {
		return new StaticMemberCompilingExpression(compiler, position, type, name);
	}

	@Override
	public CompilingExpression assign(CompilingExpression value) {
		return new InvalidCompilingExpression(compiler, position, CompileErrors.cannotUseTypeAsValue());
	}

	@Override
	public Expression as(TypeID type) {
		return compiler.at(position).invalid(CompileErrors.cannotUseTypeAsValue(), type);
	}

	@Override
	public void collect(SSAVariableCollector collector) {}

	@Override
	public void linkVariables(CodeBlockStatement.VariableLinker linker) {}
}
