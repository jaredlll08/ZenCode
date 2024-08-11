package org.openzen.zenscript.scripting;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.lexer.ParseException;
import org.openzen.zenscript.lexer.ZSTokenParser;
import org.openzen.zenscript.lexer.ZSTokenType;
import org.openzen.zenscript.parser.BracketExpressionParser;
import org.openzen.zenscript.parser.expression.ParsedExpression;
import org.openzen.zenscript.parser.expression.ParsedExpressionNull;

public class BasicBracketExpressionParser implements BracketExpressionParser {

	@Override
	public ParsedExpression parse(CodePosition position, ZSTokenParser tokens) throws ParseException {
		StringBuilder string = new StringBuilder();
		while (tokens.optional(ZSTokenType.T_GREATER) == null) {
			ZSTokenType peekType = tokens.peek().getType();
			if(peekType == ZSTokenType.EOF) {
				throw new ParseException(position, "Reached EOF, BEP is missing a closing >");
			}
			if(tokens.getLastWhitespace().contains("\n")) {
				throw new ParseException(position, "BEPs cannot contain new lines!");
			}
			string.append(tokens.next().content);
			string.append(tokens.getLastWhitespace());
		}
		return new ParsedExpressionNull(position);
	}
}
