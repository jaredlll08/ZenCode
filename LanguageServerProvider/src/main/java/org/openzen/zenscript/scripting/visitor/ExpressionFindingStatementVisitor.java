package org.openzen.zenscript.scripting.visitor;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.expression.Expression;
import org.openzen.zenscript.codemodel.statement.*;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ExpressionFindingStatementVisitor implements StatementVisitor<Optional<Expression>> {
	private static final Logger LOG = Logger.getGlobal();

	/**
	 * The found expression should contain this position
	 */
	private final CodePosition queriedPosition;
	private final ExpressionFindingExpressionVisitor expressionFindingExpressionVisitor;

	public ExpressionFindingStatementVisitor(CodePosition queriedPosition) {
		this.queriedPosition = queriedPosition;
		this.expressionFindingExpressionVisitor = new ExpressionFindingExpressionVisitor(queriedPosition);
	}

	@Override
	public Optional<Expression> visitBlock(BlockStatement statement) {
		return checkStatements(statement.statements);
	}

	@Override
	public Optional<Expression> visitBreak(BreakStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<Expression> visitContinue(ContinueStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<Expression> visitDoWhile(DoWhileStatement statement) {
		return mergeOptionals(
				checkExpression(statement.condition),
				checkStatements(statement.content)
		);
	}

	@Override
	public Optional<Expression> visitEmpty(EmptyStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<Expression> visitExpression(ExpressionStatement statement) {
		return checkExpression(statement.expression);
	}

	@Override
	public Optional<Expression> visitForeach(ForeachStatement statement) {

		return mergeOptionals(
				checkExpression(statement.list),
				checkStatements(statement.getContent())
		);
	}

	@Override
	public Optional<Expression> visitIf(IfStatement statement) {
		return mergeOptionals(
				checkExpression(statement.condition),
				checkStatements(statement.onThen, statement.onElse)
		);
	}

	@Override
	public Optional<Expression> visitLock(LockStatement statement) {

		return mergeOptionals(checkExpression(statement.object), checkStatements(statement.content));
	}

	@Override
	public Optional<Expression> visitReturn(ReturnStatement statement) {
		return checkExpression(statement.value);
	}

	@Override
	public Optional<Expression> visitSwitch(SwitchStatement statement) {
		return mergeOptionals(Stream.concat(
				Stream.of(checkExpression(statement.value)),
				statement.cases.stream()
						.map(switchCase -> switchCase.statements)
						.flatMap(Arrays::stream)
						.map(this::checkStatements)
				)
		);
	}

	@Override
	public Optional<Expression> visitThrow(ThrowStatement statement) {
		return checkExpression(statement.value);
	}

	@Override
	public Optional<Expression> visitTryCatch(TryCatchStatement statement) {
		return mergeOptionals(Stream.concat(
				Stream.of(checkStatements(statement.content, statement.finallyClause)),
				statement.catchClauses.stream().map(clause -> checkStatements(clause.content))
		));

	}

	@Override
	public Optional<Expression> visitInvalid(InvalidStatement statement) {
		LOG.log(Level.WARNING, "Invalid Statement", statement.error.code);
		return Optional.empty();
	}

	@Override
	public Optional<Expression> visitVar(VarStatement statement) {
		return checkExpression(statement.initializer);
	}

	@Override
	public Optional<Expression> visitWhile(WhileStatement statement) {
		return mergeOptionals(
				checkExpression(statement.condition),
				checkStatements(statement.content)
		);
	}

	private Optional<Expression> checkStatements(Statement... statements) {
		return Arrays.stream(statements)
				.filter(Objects::nonNull)
				.filter(statement -> statement.position.containsFully(queriedPosition))
				.map(statement -> statement.accept(ExpressionFindingStatementVisitor.this))
				.findAny()
				.flatMap(Function.identity());
	}

	private Optional<Expression> checkExpression(Expression expression) {
		if (expression == null || !expression.position.containsFully(queriedPosition)) {

			return Optional.empty();
		}

		return expression.accept(expressionFindingExpressionVisitor);
	}

	@SafeVarargs
	private final Optional<Expression> mergeOptionals(Optional<Expression>... optionals) {
		return mergeOptionals(Arrays.stream(optionals));
	}

	private Optional<Expression> mergeOptionals(Stream<Optional<Expression>> optionals) {
		return optionals.filter(Optional::isPresent).findAny().flatMap(Function.identity());
	}
}
