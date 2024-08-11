package org.openzen.zenscript.semantics;

import org.eclipse.lsp4j.SemanticTokenModifiers;

public enum SemanticTokenModifier {
	DEFINITION(1, SemanticTokenModifiers.Definition),
	READONLY(2, SemanticTokenModifiers.Readonly),
	STATIC(4, SemanticTokenModifiers.Static);

	private final int flag;
	private final String internalName;

	SemanticTokenModifier(int flag, String internalName) {
		this.flag = flag;
		this.internalName = internalName;
	}

	public int flag() {
		return flag;
	}

	public String internalName() {
		return internalName;
	}
}