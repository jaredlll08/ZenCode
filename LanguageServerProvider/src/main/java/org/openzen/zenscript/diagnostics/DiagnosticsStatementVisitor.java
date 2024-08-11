package org.openzen.zenscript.diagnostics;

import org.eclipse.lsp4j.Diagnostic;
import org.openzen.zenscript.OpenFileInfo;
import org.openzen.zenscript.codemodel.expression.Expression;
import org.openzen.zenscript.codemodel.statement.*;
import org.openzen.zenscript.codemodel.type.InvalidTypeID;
import org.openzen.zenscript.codemodel.type.TypeID;

import java.util.List;

public class DiagnosticsStatementVisitor implements StatementVisitor<Void> {

	private final List<Diagnostic> diagnostics;
	private DiagnosticsExpressionVisitor expressionVisitor;
	private DiagnosticsTypeVisitor typeVisitor;

	public DiagnosticsStatementVisitor(List<Diagnostic> diagnostics) {
		this.diagnostics = diagnostics;
	}

	private void statement(Statement... statement) {
		if (statement != null) {
			for (Statement statement1 : statement) {
				if (statement1 != null) {
					statement1.accept(this);
				}
			}
		}
	}

	private void expression(Expression... expression) {
		if (this.expressionVisitor == null) {
			this.expressionVisitor = new DiagnosticsExpressionVisitor(this.diagnostics);
		}
		if (expression != null) {
			for (Expression expression1 : expression) {
				if (expression1 != null) {
					expression1.accept(expressionVisitor);
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

	@Override
	public Void visitBlock(BlockStatement statement) {
		for (Statement statement1 : statement.statements) {
			statement(statement1);
		}
		return null;
	}

	@Override
	public Void visitBreak(BreakStatement statement) {

		statement(statement.target);
		return null;
	}

	@Override
	public Void visitContinue(ContinueStatement statement) {

		statement(statement.target);
		return null;
	}

	@Override
	public Void visitDoWhile(DoWhileStatement statement) {

		expression(statement.condition);
		statement(statement.content);
		return null;
	}

	@Override
	public Void visitEmpty(EmptyStatement statement) {

		return null;
	}

	@Override
	public Void visitExpression(ExpressionStatement statement) {

		expression(statement.expression);
		return null;
	}

	@Override
	public Void visitForeach(ForeachStatement statement) {

		for (VarStatement loopVariable : statement.loopVariables) {
			statement(loopVariable);
		}
		expression(statement.list);
		statement(statement.getContent());
		return null;
	}

	@Override
	public Void visitIf(IfStatement statement) {

		expression(statement.condition);
		statement(statement.onThen);
		statement(statement.onElse);
		return null;
	}

	@Override
	public Void visitLock(LockStatement statement) {

		expression(statement.object);
		statement(statement.content);
		return null;
	}

	@Override
	public Void visitReturn(ReturnStatement statement) {

		expression(statement.value);
		return null;
	}

	@Override
	public Void visitSwitch(SwitchStatement statement) {

		expression(statement.value);
		for (SwitchCase aCase : statement.cases) {
			for (Statement statement1 : aCase.statements) {
				statement(statement1);
			}
		}
		return null;
	}

	@Override
	public Void visitThrow(ThrowStatement statement) {

		expression(statement.value);
		return null;
	}

	@Override
	public Void visitTryCatch(TryCatchStatement statement) {

		statement(statement.resource);
		statement(statement.content);

		for (CatchClause catchClause : statement.catchClauses) {
			statement(catchClause.content);
			statement(catchClause.exceptionVariable);
		}
		statement(statement.finallyClause);
		return null;

	}

	@Override
	public Void visitVar(VarStatement statement) {
		expression(statement.initializer);
		type(statement.type);
		return null;
	}

	@Override
	public Void visitWhile(WhileStatement statement) {

		expression(statement.condition);
		statement(statement.content);
		return null;
	}

	@Override
	public Void visitInvalid(InvalidStatement statement) {
		diagnostics.add(new Diagnostic(OpenFileInfo.codePositionToRange(statement.position), statement.error.description));
		return null;
	}
}
