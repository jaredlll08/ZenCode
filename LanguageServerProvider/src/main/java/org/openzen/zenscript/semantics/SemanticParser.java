package org.openzen.zenscript.semantics;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.Modifiers;
import org.openzen.zenscript.codemodel.context.CompilingPackage;
import org.openzen.zenscript.codemodel.definition.ZSPackage;
import org.openzen.zenscript.codemodel.identifiers.ModuleSymbol;
import org.openzen.zenscript.lexer.ParseException;
import org.openzen.zenscript.lexer.ZSToken;
import org.openzen.zenscript.lexer.ZSTokenParser;
import org.openzen.zenscript.parser.expression.ParsedExpression;
import org.openzen.zenscript.parser.statements.ParsedStatementVar;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.openzen.zenscript.lexer.ZSTokenType.*;

public class SemanticParser {

	private final List<SemanticToken> semanticTokens = new ArrayList<>();
	private final TreeMap<CodePosition, ParsedStatementVar> variables;

	public SemanticParser(TreeMap<CodePosition, ParsedStatementVar> variables) {
		this.variables = variables;
	}

	private boolean isVariableDeclaredYet(CodePosition position, String name) {
		SortedMap<CodePosition, ParsedStatementVar> codePositionParsedStatementVarSortedMap = variables.descendingMap().tailMap(position);
		return codePositionParsedStatementVarSortedMap.values().stream().map(ParsedStatementVar::name).anyMatch(s -> s.equals(name));
	}

	public void parse(ZSTokenParser tokens) {
		try {
			CodePosition currentPos = null;
			while (true) {
				CodePosition position = tokens.getPosition();
				if (position == currentPos) {
					break;
				}
				// TODO annotations
				parseModifiers(tokens);
				if (tokens.isNext(K_IMPORT)) {
					keyword(tokens);
					parseImport(tokens);
				} else if ((tokens.optional(EOF)) != null) {
					break;
				} else if (tokens.isNext(T_COMMENT_SINGLELINE) || tokens.isNext(T_COMMENT_MULTILINE)) {
					keyword(tokens);
				} else {
					if (!parseDefinition(tokens)) {
						parseStatement(tokens);
					}
				}

				currentPos = position;
			}
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	private void parseModifiers(ZSTokenParser tokens) throws ParseException {
		while (true) {
			switch (tokens.peek().type) {
				case K_PUBLIC:
				case K_PRIVATE:
				case K_INTERNAL:
				case K_EXTERN:
				case K_ABSTRACT:
				case K_FINAL:
				case K_PROTECTED:
				case K_IMPLICIT:
				case K_VIRTUAL:
					keyword(tokens);
					break;
				default:
					return;
			}
		}
	}

	private void parseImport(ZSTokenParser tokens) throws ParseException {

		tokens.optional(T_DOT);

		CodePosition position = tokens.getPosition();
		ZSToken name = tokens.optional(T_IDENTIFIER);
		token(position, name.content.length(), SemanticTokenType.NAMESPACE);

		while (tokens.isNext(T_DOT)) {
			token(tokens);
			position = tokens.getPosition();
			name = tokens.optional(T_IDENTIFIER);
			token(position, name.content.length(), tokens.isNext(T_DOT) ? SemanticTokenType.NAMESPACE : SemanticTokenType.TYPE);
		}

		if (tokens.isNext(K_AS)) {
			keyword(tokens);
			position = tokens.getPosition();
			name = tokens.optional(T_IDENTIFIER);
			token(position, name.content.length(), SemanticTokenType.TYPE);
		}
		tokens.optional(T_SEMICOLON);
	}


	public boolean parseDefinition(ZSTokenParser tokens) throws ParseException {
		return false;
	}

	public void parseStatement(ZSTokenParser tokens) throws ParseException {
		CodePosition position = tokens.getPosition();
		ZSToken next = tokens.peek();
		switch (next.getType()) {
			case T_AOPEN:
				parseBlock(tokens);
				return;
			case K_RETURN:
				token(tokens); // return
				if (!tokens.isNext(T_SEMICOLON)) {
					parseExpression(tokens);
				}
				token(tokens); // ;
				return;
			case K_VAR:
			case K_VAL:
				keyword(tokens);
				position = tokens.getPosition();
				String name = tokens.optional(T_IDENTIFIER).content;
				token(position, name.length(), SemanticTokenType.VARIABLE);
				if (tokens.isNext(K_AS) || tokens.isNext(T_COLON)) {
					position = tokens.getPosition();
					ZSToken sep = tokens.next();
					if (sep.type == K_AS) {
						keyword(position, sep);
					}
					parseType(tokens);
				}
				if (tokens.isNext(T_ASSIGN)) {
					token(tokens); // =
					parseExpression(tokens);
				}
				tokens.optional(T_SEMICOLON);
				return;

			case K_IF:
				token(tokens); // if
				parseExpression(tokens);
				parseStatement(tokens); // onIf
				if (tokens.isNext(K_ELSE)) {
					token(tokens); // else
					parseStatement(tokens); // onElse
				}
				return;
			case K_FOR:
				token(tokens); // for
				token(tokens); // identifier

				while (tokens.isNext(T_COMMA)) {
					token(tokens); // ,
					token(tokens); // identifier
				}

				token(tokens); // in
				// TODO colour this somehow
				parseExpression(tokens); // source
				parseStatement(tokens); // content
				return;
			case K_DO:
				token(tokens); // do
				if (tokens.isNext(T_COLON)) {
					token(tokens); // :
					token(tokens); // identifier
				}
				parseStatement(tokens); // content
				token(tokens); //while
				parseExpression(tokens); // condition
				token(tokens); // ;
				return;

			case K_WHILE:
				token(tokens); // while
				if (tokens.isNext(T_COLON)) {
					token(tokens); // :
					token(tokens); // identifier
				}
				parseExpression(tokens);
				parseStatement(tokens);
				return;

			case K_LOCK:
				token(tokens); // lock
				parseExpression(tokens);
				parseStatement(tokens);
				return;

			case K_THROW:
				token(tokens);
				parseExpression(tokens);
				token(tokens); //;
				return;
			case K_TRY:
				tokens.pushMark();
				token(tokens);
				if (tokens.isNext(T_QUEST) || tokens.isNext(T_NOT)) {
					tokens.reset();
					break;
				}
				tokens.popMark();

				if (tokens.isNext(T_IDENTIFIER)) {
					token(tokens); // name - identifier
					token(tokens); // =
					parseExpression(tokens);
				}
				parseStatement(tokens);

				while (tokens.isNext(K_CATCH)) {
					token(tokens); // catch

					if (tokens.isNext(T_IDENTIFIER)) {
						token(tokens); // name
					}
					if (tokens.isNext(K_AS)) {
						token(tokens);
						parseType(tokens);
					}
					parseStatement(tokens);
				}
				if (tokens.isNext(K_FINALLY)) {
					token(tokens); // finally
					parseStatement(tokens);
				}
				return;
			case K_CONTINUE:
				token(tokens); // continue
				if (tokens.isNext(T_IDENTIFIER)) {
					token(tokens); // name
				}
				token(tokens); // ;
				return;

			case K_BREAK:
				token(tokens); // break
				if (tokens.isNext(T_IDENTIFIER)) {
					token(tokens); // name
				}
				token(tokens); // ;
				return;

			case K_SWITCH:
				token(tokens); // switch
				if (tokens.isNext(T_COLON)) {
					token(tokens); // :
					token(tokens); // name
				}
				parseExpression(tokens);
				boolean hasCase = false;
				while (!tokens.isNext(T_ACLOSE)) {
					if (tokens.isNext(K_CASE)) {
						token(tokens); // case
						parseExpression(tokens);
						token(tokens); // ;
						hasCase = true;
					} else if (tokens.isNext(K_DEFAULT)) {
						token(tokens); // default
						token(tokens); // :
						hasCase = true;
					} else if (!hasCase) {
						// TODO do we do anything here?
					} else {
						parseStatement(tokens);
					}
				}
				return;
		}
		parseExpression(tokens);
		token(tokens); // ;
	}


	private void parseBlock(ZSTokenParser tokens) throws ParseException {
		token(tokens); // {

		while (!tokens.isNext(T_ACLOSE)) {
			parseStatement(tokens); // TODO this doesn't do any firstContent stuff, do we want that?
		}
		token(tokens);// }
	}

	private void parseTypeParameter(ZSTokenParser tokens) throws ParseException {
		CodePosition position = tokens.getPosition();
		String name = tokens.next().content;
		token(position, name.length(), SemanticTokenType.TYPE);
		while (tokens.isNext(T_COLON)) {
			token(tokens);
			if (tokens.isNext(K_SUPER)) {
				token(tokens);
			}
			parseType(tokens);
		}
	}

	private void parseAllTypeParameters(ZSTokenParser tokens) throws ParseException {
		if (!tokens.isNext(T_LESS)) {
			return;
		}
		token(tokens); // <
		boolean consumeComma = false;
		do {
			if (consumeComma && tokens.isNext(T_COMMA)) {
				token(tokens); // ,
			}
			consumeComma = true;
			parseTypeParameter(tokens);
		} while (tokens.isNext(T_COMMA));

		token(tokens); // >
	}

	private boolean parseType(ZSTokenParser tokens) throws ParseException {
		switch (tokens.peek().type) {
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
				token(tokens);
				break;
			case K_FUNCTION:
				token(tokens);
				parseFunctionHeader(tokens);
				break;
			case T_IDENTIFIER:
				boolean canHaveDot = false;
				do {
					if (canHaveDot && tokens.isNext(T_DOT)) {
						token(tokens);
					}
					canHaveDot = true;
					CodePosition position = tokens.getPosition();
					// TODO this should look at types in the file somehow
					String name = tokens.next().content;
					boolean alreadyDeclared = isVariableDeclaredYet(position, name);
					token(position, name.length(), alreadyDeclared ? SemanticTokenType.VARIABLE : SemanticTokenType.TYPE); // name - identifier
					parseTypeArguments(tokens);
				} while (tokens.isNext(T_DOT));
				break;
			default:
				return false;
		}

		outer:
		while (true) {
			switch (tokens.peek().type) {
				case T_DOT2:
					token(tokens); // ..
					parseType(tokens);
					break;
				case T_SQOPEN:
					token(tokens); // [
					while (tokens.isNext(T_COMMA)) {
						token(tokens); // ,
					}
					if (tokens.isNext(T_SQCLOSE)) {
						token(tokens); // ]
					} else if (tokens.isNext(T_LESS)) {
						token(tokens); // <
						parseTypeParameter(tokens);
						token(tokens); // >
						token(tokens); // ]
					} else {
						parseType(tokens);
						token(tokens); // ]
					}
					break;
				case T_QUEST:
					token(tokens); // ?
					break;
				default:
					break outer;
			}
		}
		return true;
	}

	private void parseFunctionHeader(ZSTokenParser tokens) throws ParseException {
		parseAllTypeParameters(tokens);

		token(tokens); // (
		if (!tokens.isNext(T_BRCLOSE)) {
			boolean consumeComma = false;
			do {
				if (consumeComma && tokens.isNext(T_COMMA)) {
					token(tokens); // ,
				}
				consumeComma = true;
				parseAnnotations(tokens);
				token(tokens);  // argument name - identifier
				if (tokens.isNext(T_DOT3)) {
					token(tokens);  // variadic
				}
				if (tokens.isNext(K_AS) || tokens.isNext(T_COLON)) {
					token(tokens);
					parseType(tokens);
				}
				if (tokens.isNext(T_ASSIGN)) {
					parseExpression(tokens);
				}
			} while (tokens.isNext(T_COMMA));
		}
		token(tokens); // )

		if (tokens.isNext(K_AS) || tokens.isNext(T_COLON)) {
			token(tokens);
			parseType(tokens);
		}

		if (tokens.isNext(K_THROWS)) {
			token(tokens);
			parseType(tokens);
		}

	}

	private void parseAnnotations(ZSTokenParser tokens) {

	}

	//region ParsedExpression
	private void parseExpression(ZSTokenParser tokens) throws ParseException {
		parseExpression(tokens, ParsedExpression.ParsingOptions.DEFAULT);
	}

	private void parseExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		parseAssignExpression(tokens, options);
	}

	private void parseAssignExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		CodePosition position = tokens.getPosition();
		parseConditionalExpression(tokens, options);

		switch (tokens.peek().getType()) {
			case T_ASSIGN:
			case T_ADDASSIGN:
			case T_SUBASSIGN:
			case T_CATASSIGN:
			case T_MULASSIGN:
			case T_DIVASSIGN:
			case T_MODASSIGN:
			case T_ORASSIGN:
			case T_ANDASSIGN:
			case T_XORASSIGN:
			case T_SHLASSIGN:
			case T_SHRASSIGN:
			case T_USHRASSIGN:
				token(tokens); // operator
				parseAssignExpression(tokens, options);
		}
	}

	private void parseConditionalExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		parseOrOrExpression(tokens, options);

		if (tokens.isNext(T_QUEST)) {
			token(tokens); // ?
			parseOrOrExpression(tokens, options);
			token(tokens); // :
			parseConditionalExpression(tokens, options);
		}
	}

	private void parseOrOrExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		parseAndAndExpression(tokens, options);
		while (tokens.isNext(T_OROR)) {
			token(tokens); // ||
			parseAndAndExpression(tokens, options);
		}
		while (tokens.isNext(T_COALESCE)) {
			token(tokens); // ??
			parseAndAndExpression(tokens, options);
		}
	}

	private void parseAndAndExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		parseOrExpression(tokens, options);
		while (tokens.isNext(T_ANDAND)) {
			token(tokens); // &&
			parseOrExpression(tokens, options);
		}
	}

	private void parseOrExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		parseXorExpression(tokens, options);
		while (tokens.isNext(T_OR)) {
			token(tokens); // |
			parseXorExpression(tokens, options);
		}
	}

	private void parseXorExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		parseAndExpression(tokens, options);
		while (tokens.isNext(T_XOR)) {
			token(tokens); // ^
			parseAndExpression(tokens, options);
		}
	}

	private void parseAndExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		parseCompareExpression(tokens, options);
		while (tokens.isNext(T_AND)) {
			token(tokens); // &
			parseCompareExpression(tokens, options);
		}
	}

	private void parseCompareExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		parseShiftExpression(tokens, options);
		switch (tokens.peek().getType()) {
			case T_EQUAL2:
			case T_EQUAL3:
			case T_NOTEQUAL:
			case T_NOTEQUAL2:
			case T_LESS:
			case T_LESSEQ:
			case T_GREATER:
			case T_GREATEREQ:
			case K_IN:
				token(tokens); // token
				parseShiftExpression(tokens, options);
				return;
			case K_IS:
				token(tokens); // is
				parseType(tokens);
				return;
			case T_NOT:
				token(tokens); // not
				if (tokens.isNext(K_IN)) {
					token(tokens); // in
					parseShiftExpression(tokens, options);
				} else if (tokens.isNext(K_IS)) {
					token(tokens); // is
					parseType(tokens);
				}
		}
	}

	private void parseShiftExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		parseAddExpression(tokens, options);

		while (tokens.isNext(T_SHL) || tokens.isNext(T_SHR) || tokens.isNext(T_USHR)) {
			token(tokens); // << | >> | >>>
			parseAddExpression(tokens, options);
		}
	}

	private void parseAddExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		parseMulExpression(tokens, options);

		while (true) {
			if (tokens.isNext(T_ADD) || tokens.isNext(T_SUB) || tokens.isNext(T_CAT)) {
				token(tokens); // + | - | ~
				parseMulExpression(tokens, options);
			} else {
				final ZSToken peek = tokens.peek();
				if (peek.content.startsWith("-") && !peek.content.equals("-=") && peek.content.length() >= 2) {
					tokens.replace(new ZSToken(peek.type, peek.content.substring(1)));
					parseMulExpression(tokens, options);
				} else {
					break;
				}
			}
		}
	}

	private void parseMulExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		parseUnaryExpression(tokens, options);
		while (tokens.isNext(T_MUL) || tokens.isNext(T_DIV) || tokens.isNext(T_MOD)) {
			token(tokens);
			parseUnaryExpression(tokens, options);
		}
	}

	private void parseUnaryExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		switch (tokens.peek().getType()) {
			case T_NOT:
			case T_SUB:
			case T_CAT:
			case T_INCREMENT:
			case T_DECREMENT:
				token(tokens); // ! | - | ~ | ++ | --
				parseUnaryExpression(tokens, options);
				return;
			case K_TRY:
				token(tokens); // try
				if (tokens.isNext(T_QUEST)) {
					token(tokens); // ?
					parseUnaryExpression(tokens, options);
				} else if (tokens.isNext(T_NOT)) {
					parseUnaryExpression(tokens, options);
				}
			default:
				parsePostfixExpression(tokens, options);
		}
	}

	private void parsePostfixExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		parsePrimaryExpression(tokens, options);

		while (true) {
			if (tokens.isNext(T_DOT)) {
				token(tokens); // .
				if (tokens.isNext(T_IDENTIFIER)) {
					token(tokens); // indexString
					parseTypeArguments(tokens);
				} else if (tokens.isNext(T_DOLLAR)) {
					token(tokens); // $
				} else {
					if (tokens.isNext(T_STRING_SQ) || tokens.isNext(T_STRING_DQ)) {
						// TODO confirm this
						token(tokens); // ' | "
					} else {
						//TODO ParsedExpression throws a ParseException here
					}
				}
			} else if (tokens.isNext(T_DOT2)) {
				token(tokens); // ..
				parseAssignExpression(tokens, options);
			} else if (tokens.isNext(T_SQOPEN)) {
				token(tokens); // [
				boolean consumeComma = false;
				do {
					if (consumeComma && tokens.isNext(T_COMMA)) {
						token(tokens); // ,
					}
					consumeComma = true;
					parseAssignExpression(tokens, options);
				} while (tokens.isNext(T_COMMA));
				token(tokens); // ]
			} else if (tokens.isNext(T_BROPEN)) {
				token(tokens); // (
				parseCallArguments(tokens);
			} else if (tokens.isNext(K_AS)) {
				token(tokens); // as
				if (tokens.isNext(T_QUEST)) {
					token(tokens); // ?
				}
				parseType(tokens);
			} else if (tokens.isNext(T_INCREMENT) || tokens.isNext(T_DECREMENT)) {
				token(tokens); // ++ | --
			} else if (options.allowLambda && tokens.isNext(T_LAMBDA)) {
				token(tokens); // =>
				parseLambdaBody(tokens, true);
			} else {
				break;
			}
		}
	}

	private void parsePrimaryExpression(ZSTokenParser tokens, ParsedExpression.ParsingOptions options) throws ParseException {
		switch (tokens.peek().getType()) {
			case T_INT:
			case T_FLOAT:
				token(tokens); // int | float
				return;
			case T_PREFIXED_INT:
				//TODO
				return;
			case T_STRING_SQ:
			case T_STRING_DQ:
				token(tokens); // string
				return;
			case T_IDENTIFIER:
				CodePosition position = tokens.getPosition();
				String name = tokens.next().content;
				boolean alreadyDeclared = isVariableDeclaredYet(position, name);
				token(position, name.length(), alreadyDeclared ? SemanticTokenType.VARIABLE : SemanticTokenType.TYPE); // name - identifier
				parseTypeArguments(tokens);
				return;
			case T_LOCAL_IDENTIFIER:
				token(tokens); // local ident
				return;
			case K_THIS:
			case K_SUPER:
			case T_DOLLAR:
				token(tokens); // this | super | $
				return;
			case T_SQOPEN:
				token(tokens); // [
				while (tokens.isNext(T_SQCLOSE)) {
					parseAssignExpression(tokens, options);
					if (tokens.isNext(T_COMMA)) {
						token(tokens); // ,
					} else {
						break;
					}
				}
				token(tokens); // ]
				return;
			case T_AOPEN:
				//TODO confirm this still functions the same
				token(tokens);
				while (!tokens.isNext(T_ACLOSE)) {
					parseAssignExpression(tokens, options);
					if (tokens.isNext(T_COLON)) {
						token(tokens); // :
					} else {
						parseAssignExpression(tokens, options);
					}

					if (tokens.isNext(T_COMMA)) {
						token(tokens); // ,
					}
				}
				token(tokens); // }
				return;
			case K_TRUE:
			case K_FALSE:
			case K_NULL:
				token(tokens); // true | false | null
				return;
			case T_BROPEN:
				token(tokens); // (
				boolean consumeComma = false;
				do {
					if (consumeComma && tokens.isNext(T_COMMA)) {
						token(tokens); // ,
					}
					consumeComma = true;
					if (tokens.isNext(T_BRCLOSE)) {
						break;
					}
					parseAssignExpression(tokens, options);
				} while (tokens.isNext(T_COMMA));
				token(tokens); // )
				return;
			case K_NEW:
				token(tokens); // new
				parseType(tokens);
				if (tokens.isNext(T_BROPEN) || tokens.isNext(T_LESS)) {
					parseCallArguments(tokens);
				}
				return;
			case K_THROW:
			case K_PANIC:
				token(tokens); // throw | panic
				parseExpression(tokens);
				return;
			case K_MATCH:
				token(tokens); // match
				parseExpression(tokens);
				token(tokens); // {

				while (tokens.isNext(T_ACLOSE)) {
					if (tokens.isNext(K_DEFAULT)) {
						token(tokens);
					} else {
						parseExpression(tokens, new ParsedExpression.ParsingOptions(false));
					}

					token(tokens); // =>
					parseExpression(tokens);
					if (tokens.isNext(T_COMMA)) {
						token(tokens); // ,
					} else {
						break;
					}
				}
				token(tokens); // }
				return;
			case T_LESS:
				//TODO
				token(tokens); // <
				while (!tokens.isNext(T_GREATER)) {
					if (tokens.isNext(T_COLON)) {
						token(tokens);
					} else {
						CodePosition position1 = tokens.getPosition();
						ZSToken next = tokens.next();
						token(position1, next.content.length(), SemanticTokenType.EVENT);

					}
				}
				token(tokens); // >
				return;
			default:
				parseType(tokens);
				return;
		}

	}

	//endregion

	//region ParsedCallArguments
	private void parseCallArguments(ZSTokenParser tokens) {

	}

	//endregion


	private void parseLambdaBody(ZSTokenParser tokens, boolean inExpression) {

	}

	private void parsePrefixed(ZSTokenParser tokens) {

	}

	private void parseTypeArguments(ZSTokenParser tokens) throws ParseException {
		if (!tokens.isNext(T_LESS)) {
			return;
		}
		tokens.pushMark();
		token(tokens); // <
		boolean consumeComma = false;
		do {
			if (consumeComma && tokens.isNext(T_COMMA)) {
				token(tokens); // ,
			}
			consumeComma = true;
			if (!parseType(tokens)) {
				tokens.reset();
				return;
			}
		} while (tokens.isNext(T_COMMA));

		if (tokens.isNext(T_SHR)) {
			tokens.replace(T_GREATER.flyweight);
		} else if (tokens.isNext(T_USHR)) {
			tokens.replace(T_SHR.flyweight);
		} else if (tokens.isNext(T_SHRASSIGN)) {
			tokens.replace(T_GREATEREQ.flyweight);
		} else if (tokens.isNext(T_USHRASSIGN)) {
			tokens.replace(T_SHRASSIGN.flyweight);
		} else if (tokens.optional(T_GREATER) == null) {
			tokens.reset();
			return;
		}
		tokens.popMark();

	}

	private void token(CodePosition position, int length, SemanticTokenType type) {
		semanticTokens.add(new SemanticToken(position, length, type));
	}

	private void token(ZSTokenParser tokens) throws ParseException {
		semanticTokens.add(new SemanticToken(tokens.getPosition(), tokens.next()));
	}

	private void keyword(CodePosition position, ZSToken token) {
		semanticTokens.add(new SemanticToken(position, token));
	}

	private void keyword(ZSTokenParser tokens) throws ParseException {
		keyword(tokens.getPosition(), tokens.next());
	}

	public List<SemanticToken> tokens() {
		return semanticTokens;
	}
}
