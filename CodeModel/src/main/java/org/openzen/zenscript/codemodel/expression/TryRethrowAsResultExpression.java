/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.codemodel.expression;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.scope.TypeScope;
import org.openzen.zenscript.codemodel.type.ITypeID;

/**
 *
 * @author Hoofdgebruiker
 */
public class TryRethrowAsResultExpression extends Expression {
	public final Expression value;
	
	public TryRethrowAsResultExpression(CodePosition position, ITypeID type, Expression value) {
		super(position, type, null);
		
		this.value = value;
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor) {
		return visitor.visitTryRethrowAsResult(this);
	}

	@Override
	public <C, R> R accept(C context, ExpressionVisitorWithContext<C, R> visitor) {
		return visitor.visitTryRethrowAsResult(context, this);
	}

	@Override
	public Expression transform(ExpressionTransformer transformer) {
		Expression tValue = value.transform(transformer);
		return tValue == value ? this : new TryRethrowAsResultExpression(position, type, tValue);
	}

	@Override
	public Expression normalize(TypeScope scope) {
		return new TryRethrowAsResultExpression(position, type, value.normalize(scope));
	}
}
