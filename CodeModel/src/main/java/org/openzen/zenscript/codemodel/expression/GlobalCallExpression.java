/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.codemodel.expression;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.scope.TypeScope;

/**
 *
 * @author Hoofdgebruiker
 */
public class GlobalCallExpression extends Expression {
	public final String name;
	public final CallArguments arguments;
	public final Expression resolution;
	
	public GlobalCallExpression(CodePosition position, String name, CallArguments arguments, Expression resolution) {
		super(position, resolution.type, resolution.thrownType);
		
		this.name = name;
		this.arguments = arguments;
		this.resolution = resolution;
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor) {
		return visitor.visitGlobalCall(this);
	}

	@Override
	public <C, R> R accept(C context, ExpressionVisitorWithContext<C, R> visitor) {
		return visitor.visitGlobalCall(context, this);
	}

	@Override
	public Expression transform(ExpressionTransformer transformer) {
		CallArguments tArguments = arguments.transform(transformer);
		Expression tResolution = resolution.transform(transformer);
		return tArguments == arguments && tResolution == resolution ? this : new GlobalCallExpression(position, name, tArguments, tResolution);
	}

	@Override
	public Expression normalize(TypeScope scope) {
		return new GlobalCallExpression(position, name, arguments, resolution.normalize(scope));
	}
}
