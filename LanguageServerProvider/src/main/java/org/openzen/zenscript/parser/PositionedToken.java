package org.openzen.zenscript.parser;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.lexer.Token;
import org.openzen.zenscript.lexer.TokenType;

import java.util.Objects;

public class PositionedToken<TT extends TokenType, T extends Token<TT>> implements Token<TT> {

	private final CodePosition position;
	private final T delegate;

	public PositionedToken(CodePosition position, T delegate) {
		this.position = position;
		this.delegate = delegate;
	}

	public CodePosition position() {
		return position;
	}

	public T delegate() {
		return delegate;
	}

	@Override
	public TT getType() {
		return delegate.getType();
	}

	@Override
	public String getContent() {
		return delegate.getContent();
	}

	@Override
	public String toString() {
		return "PositionedToken{" +
				"position=" + position +
				", delegate=" + delegate +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PositionedToken<?, ?> that = (PositionedToken<?, ?>) o;
		return Objects.equals(position, that.position) && Objects.equals(delegate, that.delegate);
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(position);
		result = 31 * result + Objects.hashCode(delegate);
		return result;
	}
}
