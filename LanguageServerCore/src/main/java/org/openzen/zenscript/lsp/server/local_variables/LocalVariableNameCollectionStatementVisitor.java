package org.openzen.zenscript.lsp.server.local_variables;

import org.openzen.zenscript.codemodel.statement.*;

import java.util.Arrays;
import java.util.Set;

public class LocalVariableNameCollectionStatementVisitor implements StatementVisitorWithContext<Set<VarStatement>, Void> {
	@Override
	public Void visitBlock(Set<VarStatement> context, BlockStatement statement) {
		for (Statement statement1 : statement.statements) {
			statement1.accept(context, this);
		}
		return null;
	}

	@Override
	public Void visitBreak(Set<VarStatement> context, BreakStatement statement) {
		return null;
	}

	@Override
	public Void visitContinue(Set<VarStatement> context, ContinueStatement statement) {
		return null;
	}

	@Override
	public Void visitDoWhile(Set<VarStatement> context, DoWhileStatement statement) {
		statement.content.accept(context, this);
		return null;
	}

	@Override
	public Void visitEmpty(Set<VarStatement> context, EmptyStatement statement) {
		return null;
	}

	@Override
	public Void visitExpression(Set<VarStatement> context, ExpressionStatement statement) {
		return null;
	}

	@Override
	public Void visitForeach(Set<VarStatement> context, ForeachStatement statement) {
		context.addAll(Arrays.asList(statement.loopVariables));
		statement.content.accept(context, this);
		return null;
	}

	@Override
	public Void visitIf(Set<VarStatement> context, IfStatement statement) {
		statement.onThen.accept(context, this);
		if(statement.onElse != null){
			statement.onElse.accept(context, this);
		}

		return null;
	}

	@Override
	public Void visitLock(Set<VarStatement> context, LockStatement statement) {
		statement.content.accept(context, this);
		return null;
	}

	@Override
	public Void visitReturn(Set<VarStatement> context, ReturnStatement statement) {
		return null;
	}

	@Override
	public Void visitSwitch(Set<VarStatement> context, SwitchStatement statement) {
		for (SwitchCase aCase : statement.cases) {

			for (Statement statement1 : aCase.statements) {
				statement1.accept(context, this);
			}
		}
		return null;
	}

	@Override
	public Void visitThrow(Set<VarStatement> context, ThrowStatement statement) {
		return null;
	}

	@Override
	public Void visitTryCatch(Set<VarStatement> context, TryCatchStatement statement) {
		for (CatchClause catchClause : statement.catchClauses) {
			context.add(catchClause.exceptionVariable);
			catchClause.content.accept(context, this);
		}
		return null;
	}

	@Override
	public Void visitVar(Set<VarStatement> context, VarStatement statement) {
		context.add(statement);
		return null;
	}

	@Override
	public Void visitWhile(Set<VarStatement> context, WhileStatement statement) {
		statement.content.accept(context, this);
		return null;
	}
}
