package org.openzen.zenscript.lexer;

public class ZSToken implements Token<ZSTokenType> {
	public final ZSTokenType type;
	public final String content;

	public ZSToken(ZSTokenType type, String content) {
		if (content.isEmpty() && type != ZSTokenType.EOF)
			throw new IllegalArgumentException("Token must not be empty!");

		this.type = type;
		this.content = content;
	}

	@Override
	public ZSTokenType getType() {
		return type;
	}

	@Override
	public String getContent() {
		return content;
	}

	@Override
	public String toString() {
		return type + ":" + content;
	}

	public ZSToken delete(int offset, int characters) {
		return new ZSToken(
				ZSTokenType.INVALID,
				content.substring(0, offset) + content.substring(offset + characters));
	}

	public ZSToken insert(int offset, String value) {
		return new ZSToken(
				ZSTokenType.INVALID,
				content.substring(0, offset) + value + content.substring(offset));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ZSToken zsToken = (ZSToken) o;

		if (type != zsToken.type) {
			return false;
		}
		return content.equals(zsToken.content);
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + content.hashCode();
		return result;
	}
}
