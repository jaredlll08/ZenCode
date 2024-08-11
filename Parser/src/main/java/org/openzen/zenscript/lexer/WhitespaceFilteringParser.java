package org.openzen.zenscript.lexer;

import org.openzen.zencode.shared.CodePosition;

public class WhitespaceFilteringParser<TT extends TokenType, T extends Token<TT>> implements TokenStream<TT, T> {
	protected final TokenStream<TT, T> stream;
	protected T next;
	protected CodePosition position;
	protected CodePosition positionBeforeWhitespace;
	protected String whitespace;

	public WhitespaceFilteringParser(TokenStream<TT, T> stream) throws ParseException {
		this.stream = stream;
		advance();
	}

	public String getLastWhitespace() {
		return whitespace;
	}

	public void skipWhitespaceNewline() {
		int index = whitespace.indexOf('\n');
		if (index >= 0)
			whitespace = whitespace.substring(index + 1);
	}

	public String grabWhitespaceLine() {
		if (whitespace.contains("\n")) {
			int index = whitespace.indexOf('\n');
			String result = whitespace.substring(0, index);
			whitespace = whitespace.substring(index + 1);
			return result;
		} else {
			String result = whitespace;
			whitespace = "";
			return result;
		}
	}

	public T peek() {
		return next;
	}

	public void replace(T other) {
		next = other;
	}

	@Override
	public T next() throws ParseException {
		T result = next;
		advance();
		return result;
	}

	@Override
	public CodePosition getPosition() {
		return position;
	}

	public CodePosition getPositionBeforeWhitespace() {
		return positionBeforeWhitespace;
	}

	@Override
	public TT getEOF() {
		return stream.getEOF();
	}

	protected void advance() throws ParseException {
		whitespace = "";
		position = stream.getPosition();
		positionBeforeWhitespace = position;
		next = stream.next();

		while (next.getType().isWhitespace()) {
			whitespace += next.getContent();
			position = stream.getPosition();
			next = stream.next();
		}

	}
}
