package org.openzen.zenscript.semantics;

import org.openzen.zencode.shared.SourceFile;
import org.openzen.zenscript.lexer.*;
import org.openzen.zenscript.parser.BracketExpressionParser;

import java.io.IOException;

public class SemanticTokenParser extends ZSTokenParser {
	public SemanticTokenParser(TokenStream<ZSTokenType, ZSToken> parser, BracketExpressionParser bracketParser) throws ParseException {
		super(parser, bracketParser);
	}

	public static TokenParser<ZSToken, ZSTokenType> createRaw(SourceFile file, CharReader reader) {
		return new TokenParser<>(
				file,
				reader,
				DFA,
				ZSTokenType.EOF,
				ZSTokenType.INVALID,
				new ZSTokenFactory());
	}

	public static SemanticTokenParser create(SourceFile file, BracketExpressionParser bracketParser) throws IOException, ParseException {
		return new SemanticTokenParser(createRaw(file, new ReaderCharReader(file.open())), bracketParser);
	}

	protected void advance() throws ParseException {
		whitespace = "";
		position = stream.getPosition();
		positionBeforeWhitespace = position;
		next = stream.next();

		while (next.getType().isWhitespace()) {
			if (next.getType() == ZSTokenType.T_COMMENT_SINGLELINE) {
				break;
			}
			whitespace += next.getContent();
			position = stream.getPosition();
			next = stream.next();
		}
	}
}
