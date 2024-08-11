package org.openzen.zenscript.scripting.visitor;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.statement.*;

import java.util.Arrays;
import java.util.Set;

public class LocalVariableNameCollectionStatementVisitor implements StatementVisitorWithContext<Set<VarStatement>, Void> {
	/**
	 * All the variables found must be accessible in this position
	 */
	private final CodePosition queriedPosition;

	public LocalVariableNameCollectionStatementVisitor(CodePosition queriedPosition) {
		this.queriedPosition = queriedPosition;
	}

	@Override
	public Void visitBlock(Set<VarStatement> context, BlockStatement statement) {
		for (Statement statement1 : statement.statements) {
			if (statement1.position.containsFully(queriedPosition)) {
				statement1.accept(context, this);
			}
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
		if (statement.position.containsFully(queriedPosition)) {
			statement.content.accept(context, this);
		}
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
		if (statement.position.containsFully(queriedPosition)) {
			context.addAll(Arrays.asList(statement.loopVariables));
			statement.getContent().accept(context, this);
		}
		return null;
	}

	@Override
	public Void visitIf(Set<VarStatement> context, IfStatement statement) {

		if (statement.onThen.position.containsFully(queriedPosition)) {
			statement.onThen.accept(context, this);
		}

		if (statement.onElse != null && statement.onElse.position.containsFully(queriedPosition)) {
			statement.onElse.accept(context, this);
		}
		return null;
	}

	@Override
	public Void visitLock(Set<VarStatement> context, LockStatement statement) {
		if(statement.position.containsFully(queriedPosition)) {
			statement.content.accept(context, this);
		}
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
				if(statement1.position.containsFully(queriedPosition)) {
					statement1.accept(context, this);
				}
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
			if(catchClause.position.containsFully(queriedPosition)){
				context.add(catchClause.exceptionVariable);
				catchClause.content.accept(context, this);
			}
		}
		return null;
	}

	@Override
	public Void visitVar(Set<VarStatement> context, VarStatement statement) {
		if(queriedPosition.fromLine >= statement.position.fromLine) {
			context.add(statement);
		}

		return null;
	}

	@Override
	public Void visitWhile(Set<VarStatement> context, WhileStatement statement) {
		if(statement.position.containsFully(queriedPosition)) {
			statement.content.accept(context, this);
		}
		return null;
	}

	@Override
	public Void visitInvalid(Set<VarStatement> context, InvalidStatement statement) {
		return null;
	}
}
