package org.openzen.zenscript.parser;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zencode.shared.SourceFile;
import org.openzen.zenscript.lexer.*;

import java.io.IOException;

public class PositionalTokenParser<T extends Token<TT>, TT extends TokenType> implements TokenStream<TT, PositionedToken<TT, T>> {
	private static final CompiledDFA<ZSTokenType> DFA = CompiledDFA.createLexerDFA(ZSTokenType.values(), ZSTokenType.class);

	public static PositionalTokenParser<ZSToken, ZSTokenType> create(SourceFile file, CharReader reader) {
		return new PositionalTokenParser<>(file, reader, DFA, ZSTokenType.EOF, ZSTokenType.INVALID, new ZSTokenFactory());
	}

	private final CountingCharReader reader;
	private final CompiledDFA<TT> dfa;
	private final TT eof;
	private final TT invalid;
	private final TokenFactory<T, TT> factory;

	/**
	 * Creates a token stream using the specified reader and DFA.
	 *
	 * @param file   filename
	 * @param reader reader to read characters from
	 * @param dfa    DFA to tokenize the stream
	 * @param eof    end of file token type
	 */
	public PositionalTokenParser(SourceFile file, CharReader reader, CompiledDFA<TT> dfa, TT eof, TT invalid, TokenFactory<T, TT> factory) {
		if (eof.isWhitespace()) // important for the advance() method
			throw new IllegalArgumentException("EOF cannot be whitespace");

		this.reader = new CountingCharReader(reader, file);
		this.dfa = dfa;
		this.eof = eof;
		this.invalid = invalid;
		this.factory = factory;
	}

	/**
	 * Creates a token stream which reads data from the specified string.
	 *
	 * @param file filename
	 * @param data data to read
	 * @param dfa  DFA to tokenize the stream
	 * @param eof  end of file token type
	 */
	public PositionalTokenParser(SourceFile file, String data, CompiledDFA<TT> dfa, TT eof, TT invalid, TokenFactory<T, TT> factory) {
		this(file, new StringCharReader(data), dfa, eof, invalid, factory);
	}

	@Override
	public CodePosition getPosition() {
		return reader.getPosition();
	}

	public boolean hasNext() {
		try {
			return reader.peek() >= 0;
		} catch (IOException ex) {
			return false;
		}
	}

	@Override
	public TT getEOF() {
		return eof;
	}

//	private PositionedToken<TT, T> create(TT type, String content) {
//		return new PositionedToken<>(getPosition(), factory.create(type, content));
//	}

	private PositionedToken<TT, T> create(CodePosition position, TT type, String content) {
		return new PositionedToken<>(position, factory.create(type, content));
	}


	@Override
	public PositionedToken<TT, T> next() throws ParseException {
		CodePosition position = getPosition();
		try {
			if (reader.peek() < 0) return create(position,eof, "");

			int state = 0;
			StringBuilder value = new StringBuilder();
			while (dfa.transitions[state].containsKey(Math.min(reader.peek(), NFA.UNICODE_PLACEHOLDER))) {
				int c = reader.next();
				value.append((char) c);
				state = dfa.transitions[state].get(Math.min(c, NFA.UNICODE_PLACEHOLDER));
			}

			if (dfa.finals[state] != null) {
				if (state == 0) {
					value.append((char) reader.next());
					return create(position,invalid, value.toString());
				}

				return create(position,dfa.finals[state], value.toString());
			} else {
				if (reader.peek() < 0 && value.length() == 0)
					return create(position,eof, ""); // happens on comments at the end of files

				if (value.length() == 0) value.append((char) reader.next());
				return create(position,invalid, value.toString());
			}
		} catch (IOException ex) {
			throw new ParseException(getPosition(), "I/O exception: " + ex.getMessage());
		}
	}
}
