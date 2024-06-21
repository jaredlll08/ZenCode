package org.openzen.zenscript.codemodel.compilation;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zencode.shared.CompileError;
import org.openzen.zenscript.codemodel.FunctionHeader;
import org.openzen.zenscript.codemodel.GenericName;
import org.openzen.zenscript.codemodel.compilation.impl.capture.LocalExpression;
import org.openzen.zenscript.codemodel.expression.LambdaClosure;
import org.openzen.zenscript.codemodel.type.TypeID;

import java.util.List;
import java.util.Optional;

public interface ExpressionCompiler extends TypeResolver {
	ExpressionBuilder at(CodePosition position);

	CompilingExpression invalid(CodePosition position, CompileError error);

	Optional<TypeID> getThisType();

	Optional<CompilingExpression> getThis(CodePosition position);

	Optional<LocalType> getLocalType();

	Optional<TypeID> getThrowableType();

	TypeBuilder types();

	Optional<CompilingExpression> resolve(CodePosition position, GenericName name);

	Optional<CompilingExpression> dollar();

	List<String> findCandidateImports(String name);

	Optional<TypeID> union(TypeID left, TypeID right);

	ExpressionCompiler withLocalVariables(List<CompilingVariable> variables);

	ExpressionCompiler withDollar(CompilingExpression array);

	StatementCompiler forLambda(LambdaClosure closure, FunctionHeader header);
}
