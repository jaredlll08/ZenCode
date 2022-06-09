package org.openzen.zenscript.codemodel.compilation.impl.compiler;

import org.openzen.zenscript.codemodel.compilation.expression.TypeCompilingExpression;
import org.openzen.zenscript.codemodel.compilation.expression.InstanceMemberCompilingExpression;
import org.openzen.zencode.shared.CompileError;
import org.openzen.zenscript.codemodel.FunctionHeader;
import org.openzen.zenscript.codemodel.FunctionParameter;
import org.openzen.zenscript.codemodel.LocalVariable;
import org.openzen.zenscript.codemodel.compilation.*;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.GenericName;
import org.openzen.zenscript.codemodel.compilation.expression.InvalidCompilingExpression;
import org.openzen.zenscript.codemodel.definition.ExpansionDefinition;
import org.openzen.zenscript.codemodel.expression.*;
import org.openzen.zenscript.codemodel.statement.VarStatement;
import org.openzen.zenscript.codemodel.type.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExpressionCompilerImpl implements ExpressionCompiler {
	private final CompileContext context;
	private final LocalType localType;
	private final TypeID thrownType;
	private final LocalSymbols locals;
	private final FunctionHeader header;

	public ExpressionCompilerImpl(
			CompileContext context,
			LocalType localType,
			TypeID thrownType,
			LocalSymbols locals,
			FunctionHeader header
	) {
		this.context = context;
		this.localType = localType;
		this.thrownType = thrownType;
		this.locals = locals;
		this.header = header;
	}

	public ExpressionBuilder at(CodePosition position) {
		return new ExpressionBuilderImpl(position);
	}

	@Override
	public CompilingExpression invalid(CodePosition position, CompileError error) {
		return new InvalidCompilingExpression(this, position, error);
	}

	@Override
	public Optional<TypeID> getThisType() {
		return Optional.ofNullable(localType).map(LocalType::getThisType);
	}

	@Override
	public Optional<LocalType> getLocalType() {
		return Optional.ofNullable(localType);
	}

	@Override
	public Optional<TypeID> getThrowableType() {
		return Optional.ofNullable(thrownType);
	}

	@Override
	public TypeBuilder types() {
		return context;
	}

	@Override
	public Optional<CompilingExpression> resolve(CodePosition position, GenericName name) {
		Optional<VarStatement> localVariable = locals.findLocalVariable(name.name);
		if (localVariable.isPresent()) {
			return Optional.of(new LocalVariableCompiling(position, localVariable.get()));
		}

		Optional<ISymbol> global = context.findGlobal(name.name);
		if (global.isPresent())
			return global.map(g -> g.getExpression(position, name.arguments).compile(this));

		if (header != null) {
			for (FunctionParameter parameter : header.parameters) {
				if (parameter.name.equals(name.name))
					return Optional.of(new ParameterCompiling(position));
			}
		}

		return context.resolve(position, Collections.singletonList(name))
				.map(type -> new TypeCompilingExpression(this, position, type));
	}

	@Override
	public Optional<CompilingExpression> dollar() {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public List<String> findCandidateImports(String name) {
		// TODO: implement
		return Collections.emptyList();
	}

	@Override
	public Optional<TypeID> union(TypeID left, TypeID right) {
		if (left.equals(right))
			return Optional.of(right);

		ResolvedType leftResolved = resolve(left);
		ResolvedType rightResolved = resolve(right);

		if (leftResolved.canCastImplicitlyTo(right))
			return Optional.of(right);

		if (rightResolved.canCastImplicitlyTo(left))
			return Optional.of(left);

		Optional<ArrayTypeID> maybeLeftArray = left.asArray();
		Optional<ArrayTypeID> maybeRightArray = right.asArray();
		if (maybeLeftArray.isPresent() && maybeRightArray.isPresent()) {
			ArrayTypeID leftArray = maybeLeftArray.get();
			ArrayTypeID rightArray = maybeRightArray.get();

			if (leftArray.dimension == rightArray.dimension) {
				return union(leftArray.elementType, rightArray.elementType)
						.map(t -> new ArrayTypeID(t, leftArray.dimension));
			}
		}

		return Optional.empty();
	}

	@Override
	public ExpressionCompiler withLocalVariables(List<LocalVariable> variables) {

	}

	@Override
	public ExpressionCompiler forFunction(FunctionHeader header) {

	}

	@Override
	public ResolvedType resolve(TypeID type) {
		return context.resolve(type);
	}

	private class ExpressionBuilderImpl implements ExpressionBuilder {
		private final CodePosition position;

		public ExpressionBuilderImpl(CodePosition position) {
			this.position = position;
		}

		@Override
		public Expression binary(BinaryExpression.Operator operator, Expression left, Expression right) {
			return new BinaryExpression(position, left, right, operator);
		}

		@Override
		public Expression coalesce(Expression left, Expression right) {
			return new CoalesceExpression(position, left, right);
		}

		@Override
		public Expression constant(boolean value) {
			return new ConstantBoolExpression(position, value);
		}

		@Override
		public Expression constant(String value) {
			return new ConstantStringExpression(position, value);
		}

		@Override
		public Expression constant(float value) {
			return new ConstantFloatExpression(position, value);
		}

		@Override
		public Expression constant(double value) {
			return new ConstantDoubleExpression(position, value);
		}

		@Override
		public Expression constantNull(TypeID type) {
			return new NullExpression(position, type);
		}

		@Override
		public Expression getThis(TypeID thisType) {
			return new ThisExpression(position, thisType);
		}

		@Override
		public Expression invalid(CompileError error) {
			return new InvalidExpression(position, BasicTypeID.INVALID, error.code, error.description);
		}

		@Override
		public Expression invalid(CompileError error, TypeID type) {
			return new InvalidExpression(position, type, error.code, error.description);
		}

		@Override
		public Expression is(Expression value, TypeID type) {
			return new IsExpression(position, value, type);
		}

		@Override
		public Expression newArray(ArrayTypeID type, Expression[] values) {
			return new ArrayExpression(position, values, type);
		}

		@Override
		public Expression newAssoc(AssocTypeID type, List<Expression> keys, List<Expression> values) {
			return new MapExpression(position, keys.toArray(Expression.NONE), values.toArray(Expression.NONE), type);
		}

		@Override
		public Expression newGenericMap(GenericMapTypeID type) {
			throw new UnsupportedOperationException("Not yet supported");
		}

		@Override
		public Expression newRange(Expression from, Expression to) {
			return new RangeExpression(position, types().rangeOf(from.type), from, to);
		}

		@Override
		public Expression match(Expression value, TypeID resultingType, MatchExpression.Case[] cases) {
			return new MatchExpression(position, value, resultingType, cases);
		}

		@Override
		public Expression panic(TypeID type, Expression value) {
			return new PanicExpression(position, type, value);
		}

		@Override
		public Expression ternary(Expression condition, Expression ifThen, Expression ifElse) {
			return new ConditionalExpression(position, condition, ifThen, ifElse, ifThen.type);
		}

		@Override
		public Expression throw_(TypeID type, Expression value) {
			return new ThrowExpression(position, type, value);
		}

		@Override
		public Expression tryConvert(Expression value, TypeID resultingType) {
			return new TryConvertExpression(position, resultingType, value);
		}

		@Override
		public Expression tryRethrowAsException(Expression value, TypeID resultingType) {
			return new TryRethrowAsExceptionExpression(position, resultingType, value, thrownType);
		}

		@Override
		public Expression tryRethrowAsResult(Expression value, TypeID resultingType) {
			return new TryRethrowAsResultExpression(position, resultingType, value);
		}

		@Override
		public Expression unary(UnaryExpression.Operator operator, Expression value) {
			return new UnaryExpression(position, value, operator, types());
		}
	}

	private class LocalVariableCompiling implements CompilingExpression {
		private final CodePosition position;
		private final VarStatement variable;

		public LocalVariableCompiling(CodePosition position, VarStatement variable) {
			this.position = position;
			this.variable = variable;
		}

		@Override
		public Expression eval() {
			return at(position).getLocalVariable(variable);
		}

		@Override
		public CastedExpression cast(CastedEval cast) {
			return cast.of(eval());
		}

		@Override
		public Optional<StaticCallable> call() {
			return Optional.empty(); // TODO
		}

		@Override
		public CompilingExpression getMember(CodePosition position, GenericName name) {
			return new InstanceMemberCompilingExpression(ExpressionCompilerImpl.this, position, eval(), name);
		}

		@Override
		public CompilingExpression assign(CompilingExpression value) {
			return new LocalVariableCompilingAssignment(position, variable, value);
		}
	}

	private class LocalVariableCompilingAssignment implements CompilingExpression {
		private final CodePosition position;
		private final VarStatement variable;
		private final CompilingExpression value;

		public LocalVariableCompilingAssignment(CodePosition position, VarStatement variable, CompilingExpression value) {
			this.position = position;
			this.variable = variable;
			this.value = value;
		}

		@Override
		public Expression eval() {
			CastedEval cast = CastedEval.implicit(ExpressionCompilerImpl.this, position, variable.type);
			return at(position).setLocalVariable(variable, value.cast(cast).value);
		}

		@Override
		public CastedExpression cast(CastedEval cast) {
			return cast.of(eval());
		}

		@Override
		public Optional<StaticCallable> call() {
			return Optional.empty(); // TODO
		}

		@Override
		public CompilingExpression getMember(CodePosition position, GenericName name) {
			return new InstanceMemberCompilingExpression(ExpressionCompilerImpl.this, position, eval(), name);
		}

		@Override
		public CompilingExpression assign(CompilingExpression value) {
			throw new UnsupportedOperationException("To be implemented"); // TODO
		}
	}
}
