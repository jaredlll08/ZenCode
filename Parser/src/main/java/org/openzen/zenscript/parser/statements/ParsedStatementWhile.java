/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.parser.statements;

import org.openzen.zenscript.codemodel.WhitespaceInfo;
import org.openzen.zenscript.codemodel.expression.Expression;
import org.openzen.zenscript.codemodel.statement.Statement;
import org.openzen.zenscript.codemodel.statement.WhileStatement;
import org.openzen.zenscript.codemodel.type.BasicTypeID;
import org.openzen.zenscript.linker.ExpressionScope;
import org.openzen.zenscript.linker.LoopScope;
import org.openzen.zenscript.linker.StatementScope;
import org.openzen.zenscript.parser.expression.ParsedExpression;
import org.openzen.zenscript.shared.CodePosition;

/**
 *
 * @author Hoofdgebruiker
 */
public class ParsedStatementWhile extends ParsedStatement {
	public final ParsedExpression condition;
	public final ParsedStatement content;
	public final String label;
	
	public ParsedStatementWhile(CodePosition position, WhitespaceInfo whitespace, String label, ParsedExpression condition, ParsedStatement content) {
		super(position, whitespace);
		
		this.condition = condition;
		this.content = content;
		this.label = label;
	}

	@Override
	public Statement compile(StatementScope scope) {
		Expression condition = this.condition
				.compile(new ExpressionScope(scope, BasicTypeID.HINT_BOOL))
				.eval()
				.castImplicit(position, scope, BasicTypeID.BOOL);
		
		WhileStatement result = new WhileStatement(position, label, condition);
		LoopScope innerScope = new LoopScope(result, scope);
		result.content = this.content.compile(innerScope);
		return result(result);
	}
}
