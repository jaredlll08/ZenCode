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
import org.openzen.zenscript.codemodel.type.ITypeID;
import org.openzen.zenscript.codemodel.scope.TypeScope;

/**
 *
 * @author Hoofdgebruiker
 */
public class ReturnStatement extends Statement {
	public final Expression value;
	
	public ReturnStatement(CodePosition position, Expression value) {
		super(position, value == null ? null : value.thrownType);
		
		this.value = value;
	}
	
	@Override
	public ITypeID getReturnType() {
		return value.type;
	}
	
	@Override
	public void forEachStatement(Consumer<Statement> consumer) {
		consumer.accept(this);
	}
	
	@Override
	public Statement withReturnType(TypeScope scope, ITypeID returnType) {
		return new ReturnStatement(position, value == null ? null : value.castImplicit(position, scope, returnType));
	}

	@Override
	public <T> T accept(StatementVisitor<T> visitor) {
		return visitor.visitReturn(this);
	}
	
	@Override
	public <C, R> R accept(C context, StatementVisitorWithContext<C, R> visitor) {
		return visitor.visitReturn(context, this);
	}

	@Override
	public Statement transform(StatementTransformer transformer, ConcatMap<LoopStatement, LoopStatement> modified) {
		Expression tValue = value == null ? null : value.transform(transformer);
		return tValue == value ? this : new ReturnStatement(position, tValue);
	}

	@Override
	public Statement transform(ExpressionTransformer transformer, ConcatMap<LoopStatement, LoopStatement> modified) {
		Expression tValue = value == null ? null : value.transform(transformer);
		return tValue == value ? this : new ReturnStatement(position, tValue);
	}

	@Override
	public Statement normalize(TypeScope scope, ConcatMap<LoopStatement, LoopStatement> modified) {
		return new ReturnStatement(position, value == null ? null : value.normalize(scope));
	}
}
