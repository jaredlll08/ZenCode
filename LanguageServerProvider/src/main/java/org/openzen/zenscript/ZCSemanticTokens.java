package org.openzen.zenscript;

import org.eclipse.lsp4j.SemanticTokensLegend;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.lexer.ZSToken;
import org.openzen.zenscript.lexer.ZSTokenParser;
import org.openzen.zenscript.lexer.ZSTokenType;
import org.openzen.zenscript.parser.statements.ParsedStatement;
import org.openzen.zenscript.parser.statements.ParsedStatementVar;
import org.openzen.zenscript.scripting.LSPEngine;
import org.openzen.zenscript.semantics.SemanticParser;
import org.openzen.zenscript.semantics.SemanticToken;
import org.openzen.zenscript.semantics.SemanticTokenModifier;
import org.openzen.zenscript.semantics.SemanticTokenType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ZCSemanticTokens {

	public static final SemanticTokensLegend SEMANTIC_TOKENS_LEGEND = Utils.make(new SemanticTokensLegend(), (legend) -> {
		legend.setTokenTypes(Arrays.stream(SemanticTokenType.values()).map(SemanticTokenType::internalName).collect(Collectors.toList()));
		legend.setTokenModifiers(Arrays.stream(SemanticTokenModifier.values()).map(SemanticTokenModifier::internalName).collect(Collectors.toList()));
	});

	public static boolean tokenFilter(Map.Entry<CodePosition, ZSToken> token) {
		ZSTokenType type = token.getValue().getType();
		if (type == ZSTokenType.T_STRING_DQ) {
			return true;
		}
		if (type == ZSTokenType.T_COMMENT_SINGLELINE) {
			return true;
		}
		if (type == ZSTokenType.T_COMMENT_MULTILINE) {
			return true;
		}
		return type.isKeyword;
	}

	public static void basicTokens(OpenFileInfo file, List<Integer> data) {
		List<SemanticToken> tokens = file.tokensAtPosition.entrySet()
				.stream()
				.filter(ZCSemanticTokens::tokenFilter)
				.map(entry -> new SemanticToken(entry.getKey(), entry.getValue()))
				.filter(semanticToken -> semanticToken.tokenType() != null) // TODO do we need to do this?
				.collect(Collectors.toList());

		IntStream.range(0, tokens.size()).forEach(i -> {
			SemanticToken current = tokens.get(i);
			if (i == 0) {
				current.encode(data);
			} else {
				current.encode(data, tokens.get(i - 1));
			}
		});
	}

	public static void advancedTokens(LSPEngine engine, OpenFileInfo file, List<Integer> data) {
		TreeMap<CodePosition, ParsedStatementVar> variables = new TreeMap<>(CodePosition::compareTo);
		for (ParsedStatement statement : file.parsedFile.statements()) {
			if (statement instanceof ParsedStatementVar) {
				ParsedStatementVar parsedStatementVar = (ParsedStatementVar) statement;
				variables.put(statement.position, parsedStatementVar);
			}
		}
		SemanticParser parser = new SemanticParser(variables);
		ZSTokenParser tokenParser = file.getTokenParser();
		parser.parse(tokenParser);

		List<SemanticToken> tokens = parser.tokens();

		IntStream.range(0, tokens.size()).forEach(i -> {
			SemanticToken current = tokens.get(i);
			if (i == 0) {
				current.encode(data);
			} else {
				current.encode(data, tokens.get(i - 1));
			}
		});
//		try {
////			SemanticTokenParser.parse(file.getTokenParser());
////
////			List<SemanticToken> tokens = SemanticTokenParser.tokens();
////			IntStream.range(0, tokens.size()).forEach(i -> {
////				SemanticToken current = tokens.get(i);
////				if (i == 0) {
////					current.encode(data);
////				} else {
////					current.encode(data, tokens.get(i - 1));
////				}
////			});
////			tokens.clear();
//		} catch (ParseException e) {
//			throw new RuntimeException(e);
//		}
	}

	public static SemanticTokenType mapToken(ZSTokenType token) {
		switch (token) {
			case K_VOID:
			case K_BOOL:
			case K_BYTE:
			case K_SBYTE:
			case K_SHORT:
			case K_USHORT:
			case K_INT:
			case K_UINT:
			case K_LONG:
			case K_ULONG:
			case K_USIZE:
			case K_FLOAT:
			case K_DOUBLE:
			case K_CHAR:
			case K_STRING:
				return SemanticTokenType.TYPE;
			case T_BROPEN:
			case T_BRCLOSE:
			case T_AOPEN:
			case T_ACLOSE:
			case T_SQOPEN:
			case T_SQCLOSE:
			case T_COMMA:
				return SemanticTokenType.DECORATOR;
			case T_IDENTIFIER:
			case T_LOCAL_IDENTIFIER:
				return SemanticTokenType.FUNCTION;
			case T_COMMENT_SINGLELINE:
			case T_COMMENT_MULTILINE:
				return SemanticTokenType.COMMENT;
			case T_STRING_DQ:
			case T_STRING_DQ_WYSIWYG:
			case T_STRING_SQ:
			case T_STRING_SQ_WYSIWYG:
				return SemanticTokenType.STRING;
			case T_INT:
			case T_FLOAT:
			case T_PREFIXED_INT:
				return SemanticTokenType.NUMBER;
			case T_DOT3:
			case T_DOT2:
			case T_DOT:
			case T_INCREMENT:
			case T_ADDASSIGN:
			case T_ADD:
			case T_DECREMENT:
			case T_SUBASSIGN:
			case T_SUB:
			case T_CATASSIGN:
			case T_CAT:
			case T_MULASSIGN:
			case T_MUL:
			case T_DIVASSIGN:
			case T_DIV:
			case T_MODASSIGN:
			case T_MOD:
			case T_ORASSIGN:
			case T_OROR:
			case T_OR:
			case T_ANDASSIGN:
			case T_ANDAND:
			case T_AND:
			case T_XORASSIGN:
			case T_XOR:
			case T_COALESCE:
			case T_OPTCALL:
			case T_QUEST:
			case T_COLON:
			case T_LESSEQ:
			case T_SHLASSIGN:
			case T_SHL:
			case T_LESS:
			case T_GREATEREQ:
			case T_USHR:
			case T_USHRASSIGN:
			case T_SHRASSIGN:
			case T_SHR:
			case T_GREATER:
			case T_LAMBDA:
			case T_EQUAL3:
			case T_EQUAL2:
			case T_ASSIGN:
			case T_NOTEQUAL2:
			case T_NOTEQUAL:
			case T_NOT:
			case T_DOLLAR:
				return SemanticTokenType.OPERATOR;
		}
		if (token.isKeyword) {
			return SemanticTokenType.KEYWORD;
		}
		return null;
	}

}
