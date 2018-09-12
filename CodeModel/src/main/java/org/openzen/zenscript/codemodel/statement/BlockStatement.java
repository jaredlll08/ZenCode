/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.codemodel.statement;

import java.util.function.Consumer;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zencode.shared.ConcatMap;
import org.openzen.zenscript.codemodel.expression.Expression;
import org.openzen.zenscript.codemodel.expression.ExpressionTransformer;
import org.openzen.zenscript.codemodel.scope.TypeScope;
import org.openzen.zenscript.codemodel.type.ITypeID;

/**
 *
 * @author Hoofdgebruiker
 */
public class BlockStatement extends Statement {
	public final Statement[] statements;
	
	public BlockStatement(CodePosition position, Statement[] statements) {
		super(position, getThrownType(statements));
		
		this.statements = statements;
	}

	@Override
	public <T> T accept(StatementVisitor<T> visitor) {
		return visitor.visitBlock(this);
	}
	
	@Override
	public <C, R> R accept(C context, StatementVisitorWithContext<C, R> visitor) {
		return visitor.visitBlock(context, this);
	}
	
	@Override
	public void forEachStatement(Consumer<Statement> consumer) {
		consumer.accept(this);
		for (Statement s : statements) {
			s.forEachStatement(consumer);
		}
	}
	
	@Override
	public Statement transform(StatementTransformer transformer, ConcatMap<LoopStatement, LoopStatement> modified) {
		Statement[] tStatements = new Statement[statements.length];
		boolean unchanged = true;
		for (int i = 0; i < statements.length; i++) {
			Statement statement = statements[i];
			Statement tStatement = statement.transform(transformer, modified);
			unchanged &= statement == tStatement;
			tStatements[i] = statement;
		}
		return unchanged ? this : new BlockStatement(position, tStatements);
	}

	@Override
	public Statement transform(ExpressionTransformer transformer, ConcatMap<LoopStatement, LoopStatement> modified) {
		Statement[] tStatements = new Statement[statements.length];
		boolean unchanged = true;
		for (int i = 0; i < tStatements.length; i++) {
			Statement tStatement = statements[i].transform(transformer, modified);
			unchanged &= statements[i] == tStatement;
			tStatements[i] = tStatement;
		}
		return unchanged ? this : new BlockStatement(position, tStatements);
	}
	
	private static ITypeID getThrownType(Statement[] statements) {
		ITypeID result = null;
		for (Statement statement : statements)
			result = Expression.binaryThrow(statement.position, result, statement.thrownType);
		return result;
	}

	@Override
	public Statement normalize(TypeScope scope, ConcatMap<LoopStatement, LoopStatement> modified) {
		Statement[] normalized = new Statement[statements.length];
		int i = 0;
		for (Statement statement : statements)
			normalized[i++] = statement.normalize(scope, modified);
		return new BlockStatement(position, normalized);
	}
}
