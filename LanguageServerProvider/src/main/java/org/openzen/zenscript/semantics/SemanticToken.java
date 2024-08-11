package org.openzen.zenscript.semantics;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.ZCSemanticTokens;
import org.openzen.zenscript.lexer.ZSToken;
import org.openzen.zenscript.lexer.ZSTokenType;

import java.util.*;

public class SemanticToken {
	private final int line;
	private final int column;
	private final int length;
	private final SemanticTokenType tokenType;
	private final Set<SemanticTokenModifier> modifiers;
	private int relativeLine;
	private int relativeColumn;

	public static List<SemanticToken> decode(List<Integer> data) {

		List<SemanticToken> tokens = new ArrayList<>();
		int line = -1;
		int column = -1;
		int length = -1;
		SemanticTokenType type = null;
		Set<SemanticTokenModifier> modifiers = new HashSet<>();
		for (int i = 0; i < data.size(); i++) {

			switch (i % 5) {
				case 0:
					if (line != -1) {
						tokens.add(new SemanticToken(line, column, length, type, modifiers));
					}
					line = data.get(i);
					break;
				case 1:
					column = data.get(i);
					break;
				case 2:
					length = data.get(i);
					break;
				case 3:
					type = SemanticTokenType.values()[data.get(i)];

			}
		}
		tokens.add(new SemanticToken(line, column, length, type, modifiers));

		return tokens;
	}

	public SemanticToken(int line, int column, int length, SemanticTokenType tokenType, Set<SemanticTokenModifier> modifiers) {
		this.line = line;
		this.column = column;
		this.length = length;
		this.tokenType = tokenType;
		this.modifiers = modifiers;
	}

	public SemanticToken(CodePosition pos, ZSToken token) {
		this(pos.fromLine - 1,
				pos.fromLineOffset,
				token.content.length(),
				ZCSemanticTokens.mapToken(token.type),
				Collections.emptySet());
	}

	public SemanticToken(CodePosition pos, ZSTokenType type) {
		this(pos.fromLine - 1,
				pos.fromLineOffset,
				type.flyweight.getContent().length(),
				ZCSemanticTokens.mapToken(type),
				Collections.emptySet());
	}

	public SemanticToken(CodePosition pos, int length, SemanticTokenType type) {
		this(pos.fromLine - 1,
				pos.fromLineOffset,
				length,
				type,
				Collections.emptySet());
	}

	public void encode(List<Integer> tokens) {
		encode(tokens, null);
	}

	public void encode(List<Integer> tokens, SemanticToken previous) {
		if (tokenType == null) {
			return;
		}
		this.relativeLine = line();
		this.relativeColumn = this.column();
		if (previous != null) {
			if (line() == previous.line()) {
				relativeColumn -= previous.column();
			}
			relativeLine -= previous.line();
		}
		tokens.add(relativeLine);
		tokens.add(relativeColumn);
		tokens.add(this.length());
		tokens.add(this.tokenType() == null ? 0 : this.tokenType().ordinal());
		tokens.add(modifiers.stream().map(SemanticTokenModifier::flag).reduce((mod1, mod2) -> mod1 | mod2).orElse(0));
	}

	public int line() {
		return line;
	}

	public int column() {
		return column;
	}

	public int length() {
		return length;
	}

	public SemanticTokenType tokenType() {
		return tokenType;
	}

	public Set<SemanticTokenModifier> modifiers() {
		return modifiers;
	}



	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("SemanticToken{");
		sb.append("line=").append(line);
		sb.append(", column=").append(column);
		sb.append(", length=").append(length);
		sb.append(", tokenType=").append(tokenType);
		sb.append(", modifiers=").append(modifiers);
		sb.append('}');
		return sb.toString();
	}
}
