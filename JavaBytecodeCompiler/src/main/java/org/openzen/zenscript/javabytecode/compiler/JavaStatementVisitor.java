package org.openzen.zenscript.javabytecode.compiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.openzen.zenscript.codemodel.statement.*;
import org.openzen.zenscript.codemodel.type.BasicTypeID;
import org.openzen.zenscript.codemodel.type.RangeTypeID;
import org.openzen.zenscript.codemodel.type.builtin.BuiltinMethodSymbol;
import org.openzen.zenscript.javabytecode.BytecodeLoopLabels;
import org.openzen.zenscript.javabytecode.JavaBytecodeContext;
import org.openzen.zenscript.javabytecode.JavaLocalVariableInfo;
import org.openzen.zenscript.javashared.JavaBuiltinModule;
import org.openzen.zenscript.javabytecode.JavaMangler;
import org.openzen.zenscript.javashared.JavaCompiledModule;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class JavaStatementVisitor implements StatementVisitor<Boolean> {
	public final JavaExpressionVisitor expressionVisitor;
	public final JavaNonPushingExpressionVisitor nonPushingExpressionVisitor;
	final JavaBytecodeContext context;
	private final JavaWriter javaWriter;

	/**
	 * @param javaWriter the method writer that compiles the statement
	 */
	public JavaStatementVisitor(JavaBytecodeContext context, JavaCompiledModule module, JavaWriter javaWriter, JavaMangler javaMangler) {
		this(context, new JavaExpressionVisitor(context, module, javaWriter, javaMangler), javaMangler);
	}

	public JavaStatementVisitor(JavaBytecodeContext context, JavaExpressionVisitor expressionVisitor, JavaMangler javaMangler) {
		this.javaWriter = expressionVisitor.getJavaWriter();
		this.context = context;
		this.expressionVisitor = expressionVisitor;
		this.nonPushingExpressionVisitor = new JavaNonPushingExpressionVisitor(expressionVisitor.context, expressionVisitor.module, expressionVisitor.javaWriter, javaMangler, expressionVisitor);
	}

	@Override
	public Boolean visitBlock(BlockStatement statement) {
		javaWriter.position(statement.position.fromLine);
		Boolean returns = false;
		for (Statement statement1 : statement.statements) {
			returns = statement1.accept(this);
		}
		return returns;
	}

	@Override
	public Boolean visitBreak(BreakStatement statement) {
		javaWriter.position(statement.position.fromLine);

		Label endLabel = context.getLoopLabels(statement.target).loopEnd;
		javaWriter.goTo(endLabel);

		return false;
	}

	@Override
	public Boolean visitContinue(ContinueStatement statement) {
		javaWriter.position(statement.position.fromLine);

		Label startLabel = context.getLoopLabels(statement.target).loopStart;
		javaWriter.goTo(startLabel);

		return false;
	}

	@Override
	public Boolean visitDoWhile(DoWhileStatement statement) {
		javaWriter.position(statement.position.fromLine);
		Label start = new Label();
		Label end = new Label();

		context.setLoopLabels(statement, new BytecodeLoopLabels(start, end));

		javaWriter.label(start);
		statement.content.accept(this);

		statement.condition.accept(expressionVisitor);
		javaWriter.ifNE(start);

		//Only needed for break statements, should be nop if not used
		javaWriter.label(end);
		return false;
	}

	@Override
	public Boolean visitEmpty(EmptyStatement statement) {
		//No-Op
		return false;
	}

	@Override
	public Boolean visitExpression(ExpressionStatement statement) {
		javaWriter.position(statement.position.fromLine);
		statement.expression.accept(nonPushingExpressionVisitor);
		return false;
	}

	@Override
	public Boolean visitForeach(ForeachStatement statement) {
		javaWriter.position(statement.position.fromLine);
		//Create Labels
		Label start = new Label();
		Label end = new Label();
		context.setLoopLabels(statement, new BytecodeLoopLabels(start, end));


		//Compile Array/Collection
		statement.list.accept(expressionVisitor);

		//Create local variables
		for (VarStatement variable : statement.loopVariables) {
			final Type type = context.getType(variable.type);
			final Label variableStart = new Label();
			final JavaLocalVariableInfo info = new JavaLocalVariableInfo(type, javaWriter.local(type), variableStart, variable.name);
			info.end = end;
			javaWriter.setLocalVariable(variable.variable, info);
			javaWriter.addVariableInfo(info);
		}

		//javaWriter.label(min);
		JavaForeachWriter iteratorWriter = new JavaForeachWriter(this, statement, start, end);
		if (statement.iterator.method.method instanceof BuiltinMethodSymbol) {
			switch ((BuiltinMethodSymbol) statement.iterator.method.method) {
				case ITERATOR_INT_RANGE:
					iteratorWriter.visitIntRange(((RangeTypeID) statement.iterator.targetType));
					break;
				case ITERATOR_ARRAY_VALUES:
					iteratorWriter.visitArrayValueIterator();
					break;
				case ITERATOR_ARRAY_KEY_VALUES:
					iteratorWriter.visitArrayKeyValueIterator();
					break;
				case ITERATOR_ASSOC_KEYS:
					iteratorWriter.visitAssocKeyIterator();
					break;
				case ITERATOR_ASSOC_KEY_VALUES:
					iteratorWriter.visitAssocKeyValueIterator();
					break;
				case ITERATOR_STRING_CHARS:
					iteratorWriter.visitStringCharacterIterator();
					break;
				//case ITERATOR_VALUES:
				//	iteratorWriter.visitIteratorIterator(context.getType(statement.loopVariables[0].type));
				//	break;
				//case ITERABLE:
				//	iteratorWriter.visitCustomIterator();
				default:
					throw new IllegalArgumentException("Invalid iterator: " + statement.iterator);

			}
		} else {
			iteratorWriter.visitCustomIterator();
		}

		javaWriter.goTo(start);
		javaWriter.label(end);
		javaWriter.pop();
		return false;
	}

	@Override
	public Boolean visitIf(IfStatement statement) {
		javaWriter.position(statement.position.fromLine);
		statement.condition.accept(expressionVisitor);
		Label onElse = null;
		Label end = new Label();
		final boolean hasElse = statement.onElse != null;
		if (hasElse) {
			onElse = new Label();
			javaWriter.ifEQ(onElse);
		} else {
			javaWriter.ifEQ(end);
		}
		statement.onThen.accept(this);
		if (hasElse) {
			javaWriter.goTo(end);
			javaWriter.label(onElse);
			statement.onElse.accept(this);
		}
		javaWriter.label(end);
		return false;
	}

	@Override
	public Boolean visitLock(LockStatement statement) {
		return false;
	}

	@Override
	public Boolean visitInvalid(InvalidStatement statement) {
		throw new UnsupportedOperationException("Invalid Statement: " + statement.error.description);
	}

	@Override
	public Boolean visitReturn(ReturnStatement statement) {
		javaWriter.position(statement.position.fromLine);
		if (statement.value == null) {
			javaWriter.ret();
		} else {
			statement.value.accept(expressionVisitor);
			javaWriter.returnType(context.getType(statement.value.type));
		}

		return true;
	}

	@Override
	public Boolean visitSwitch(SwitchStatement statement) {
		javaWriter.position(statement.position.fromLine);

		final Label start = new Label();
		final Label end = new Label();
		context.setLoopLabels(statement, new BytecodeLoopLabels(start, end));

		javaWriter.label(start);
		statement.value.accept(expressionVisitor);
		if (statement.value.type == BasicTypeID.STRING)
			javaWriter.invokeVirtual(JavaMethodBytecodeCompiler.OBJECT_HASHCODE);
		if (statement.value.type.isEnum()) {
			javaWriter.invokeVirtual(JavaBuiltinModule.ENUM_ORDINAL);
		}
		boolean out = false;

		final boolean hasNoDefault = hasNoDefault(statement);

		final List<SwitchCase> cases = statement.cases;
		final JavaSwitchLabel[] switchLabels = new JavaSwitchLabel[hasNoDefault ? cases.size() : cases.size() - 1];
		final Label defaultLabel = new Label();

		int i = 0;
		for (final SwitchCase switchCase : cases) {
			if (switchCase.value != null) {
				switchLabels[i++] = new JavaSwitchLabel(CompilerUtils.getKeyForSwitch(switchCase.value), new Label());
			}
		}

		JavaSwitchLabel[] sortedSwitchLabels = Arrays.copyOf(switchLabels, switchLabels.length);
		Arrays.sort(sortedSwitchLabels, Comparator.comparingInt(a -> a.key));

		javaWriter.lookupSwitch(defaultLabel, sortedSwitchLabels);

		i = 0;
		for (final SwitchCase switchCase : cases) {
			if (hasNoDefault || switchCase.value != null) {
				javaWriter.label(switchLabels[i++].label);
			} else {
				javaWriter.label(defaultLabel);
			}
			for (Statement statement1 : switchCase.statements) {
				out |= statement1.accept(this);
			}
		}

		if (hasNoDefault)
			javaWriter.label(defaultLabel);

		javaWriter.label(end);


		//throw new UnsupportedOperationException("Not yet implemented!");
		return out;
	}

	private boolean hasNoDefault(SwitchStatement switchStatement) {
		for (SwitchCase switchCase : switchStatement.cases)
			if (switchCase.value == null) return false;
		return true;
	}

	@Override
	public Boolean visitThrow(ThrowStatement statement) {
		javaWriter.position(statement.position.fromLine);
		statement.value.accept(expressionVisitor);
		javaWriter.aThrow();
		return false;
	}

	@Override
	public Boolean visitTryCatch(TryCatchStatement statement) {
		javaWriter.position(statement.position.fromLine);
		final Label tryCatchStart = new Label();
		final Label tryFinish = new Label();
		final Label tryCatchFinish = new Label();
		final Label finallyStart = new Label();

		javaWriter.label(tryCatchStart);
		//TODO Check for returns or breaks out of the try-catch and inject finally block before them
		statement.content.accept(this);
		javaWriter.label(tryFinish);
		if (statement.finallyClause != null)
			statement.finallyClause.accept(this);
		javaWriter.goTo(tryCatchFinish);

		for (CatchClause catchClause : statement.catchClauses) {
			final Label catchStart = new Label();
			javaWriter.label(catchStart);

			catchClause.exceptionVariable.accept(this);
			final JavaLocalVariableInfo localVariable = javaWriter.getLocalVariable(catchClause.exceptionVariable.variable);
			javaWriter.store(localVariable.type, localVariable.local);

			catchClause.content.accept(this);
			final Label catchFinish = new Label();
			javaWriter.label(catchFinish);

			if (statement.finallyClause != null) {
				statement.finallyClause.accept(this);
				javaWriter.tryCatch(catchStart, catchFinish, finallyStart, null);
			}

			javaWriter.tryCatch(tryCatchStart, tryFinish, catchStart, localVariable.type.getInternalName());
			javaWriter.goTo(tryCatchFinish);
		}

		if (statement.finallyClause != null) {
			javaWriter.label(finallyStart);
			final int local = javaWriter.local(Object.class);
			javaWriter.storeObject(local);
			statement.finallyClause.accept(this);
			javaWriter.loadObject(local);
			javaWriter.aThrow();
			javaWriter.tryCatch(tryCatchStart, tryFinish, finallyStart, null);
		}
		javaWriter.label(tryCatchFinish);

		return false;
	}

	@Override
	public Boolean visitVar(VarStatement statement) {
		javaWriter.position(statement.position.fromLine);
		if (statement.initializer != null) {
			statement.initializer.accept(expressionVisitor);
		}

		Type type = context.getType(statement.type);
		int local = javaWriter.local(type);
		if (statement.initializer != null)
			javaWriter.store(type, local);
		final Label variableStart = new Label();
		javaWriter.label(variableStart);
		final JavaLocalVariableInfo info = new JavaLocalVariableInfo(type, local, variableStart, statement.name);
		javaWriter.setLocalVariable(statement.variable, info);
		javaWriter.addVariableInfo(info);
		return false;
	}

	@Override
	public Boolean visitWhile(WhileStatement statement) {
		javaWriter.position(statement.position.fromLine);
		Label start = new Label();
		Label end = new Label();
		context.setLoopLabels(statement, new BytecodeLoopLabels(start, end));

		javaWriter.label(start);
		statement.condition.accept(expressionVisitor);
		javaWriter.ifEQ(end);
		statement.content.accept(this);
		javaWriter.goTo(start);
		javaWriter.label(end);
		return false;
	}

	public void start() {
		javaWriter.start();
	}

	public void end() {
		javaWriter.ret();
		javaWriter.end();
	}

	public JavaWriter getJavaWriter() {
		return javaWriter;
	}
}
