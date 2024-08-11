package org.openzen.zenscript.semantics;

import org.eclipse.lsp4j.SemanticTokenTypes;

public enum SemanticTokenType {
	NAMESPACE(SemanticTokenTypes.Namespace),
	TYPE(SemanticTokenTypes.Type),
	CLASS(SemanticTokenTypes.Class),
	ENUM(SemanticTokenTypes.Enum),
	INTERFACE(SemanticTokenTypes.Interface),
	STRUCT(SemanticTokenTypes.Struct),
	TYPE_PARAMETER(SemanticTokenTypes.TypeParameter),
	PARAMETER(SemanticTokenTypes.Parameter),
	VARIABLE(SemanticTokenTypes.Variable),
	PROPERTY(SemanticTokenTypes.Property),
	ENUM_MEMBER(SemanticTokenTypes.EnumMember),
	EVENT(SemanticTokenTypes.Event),
	FUNCTION(SemanticTokenTypes.Function),
	METHOD(SemanticTokenTypes.Method),
	MACRO(SemanticTokenTypes.Macro),
	KEYWORD(SemanticTokenTypes.Keyword),
	MODIFIER(SemanticTokenTypes.Modifier),
	COMMENT(SemanticTokenTypes.Comment),
	STRING(SemanticTokenTypes.String),
	NUMBER(SemanticTokenTypes.Number),
	REGEXP(SemanticTokenTypes.Regexp),
	OPERATOR(SemanticTokenTypes.Operator),
	DECORATOR(SemanticTokenTypes.Decorator);

	private final String internalName;

	SemanticTokenType(String internalName) {
		this.internalName = internalName;
	}

	public String internalName() {
		return internalName;
	}
}