package org.openzen.zenscript.parser.expression;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zencode.shared.CompileException;
import org.openzen.zencode.shared.CompileExceptionCode;
import org.openzen.zencode.shared.StringExpansion;
import org.openzen.zenscript.codemodel.CompareType;
import org.openzen.zenscript.codemodel.OperatorType;
import org.openzen.zenscript.codemodel.expression.Expression;
import org.openzen.zenscript.codemodel.expression.switchvalue.SwitchValue;
import org.openzen.zenscript.codemodel.partial.IPartialExpression;
import org.openzen.zenscript.codemodel.scope.BaseScope;
import org.openzen.zenscript.codemodel.scope.ExpressionScope;
import org.openzen.zenscript.codemodel.type.TypeID;
import org.openzen.zenscript.lexer.ParseException;
import org.openzen.zenscript.lexer.ZSToken;
import org.openzen.zenscript.lexer.ZSTokenParser;
import org.openzen.zenscript.lexer.ZSTokenType;
import org.openzen.zenscript.parser.definitions.ParsedFunctionHeader;
import org.openzen.zenscript.parser.definitions.ParsedFunctionParameter;
import org.openzen.zenscript.parser.statements.ParsedFunctionBody;
import org.openzen.zenscript.parser.statements.ParsedStatement;
import org.openzen.zenscript.parser.type.IParsedType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.openzen.zencode.shared.StringExpansion.unescape;
import static org.openzen.zenscript.lexer.ZSTokenType.*;

public abstract class ParsedExpression {
	public final CodePosition position;

	public ParsedExpression(CodePosition position) {
		this.position = position;
	}

	public static ParsedExpression parse(ZSTokenParser parser) throws ParseException {
		return readAssignExpression(parser, ParsingOptions.DEFAULT);
	}

	public static ParsedExpression parse(ZSTokenParser parser, ParsingOptions options) throws ParseException {
		return readAssignExpression(parser, options);
	}

	private static ParsedExpression readAssignExpression(ZSTokenParser parser, ParsingOptions options) throws ParseException {
		CodePosition position = parser.getPosition();
		ParsedExpression left = readConditionalExpression(position, parser, options);

		final ParsedExpression right;
		switch (parser.peek().type) {
			case T_ASSIGN:
				parser.next();
				right = readAssignExpression(parser, options);
				return new ParsedExpressionAssign(position.until(parser.getPositionBeforeWhitespace()), left, right);
			case T_ADDASSIGN:
				parser.next();
				right = readAssignExpression(parser, options);
				return new ParsedExpressionOpAssign(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.ADDASSIGN);
			case T_SUBASSIGN:
				parser.next();
				right = readAssignExpression(parser, options);
				return new ParsedExpressionOpAssign(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.SUBASSIGN);
			case T_CATASSIGN:
				parser.next();
				right = readAssignExpression(parser, options);
				return new ParsedExpressionOpAssign(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.CATASSIGN);
			case T_MULASSIGN:
				parser.next();
				right = readAssignExpression(parser, options);
				return new ParsedExpressionOpAssign(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.MULASSIGN);
			case T_DIVASSIGN:
				parser.next();
				right = readAssignExpression(parser, options);
				return new ParsedExpressionOpAssign(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.DIVASSIGN);
			case T_MODASSIGN:
				parser.next();
				right = readAssignExpression(parser, options);
				return new ParsedExpressionOpAssign(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.MODASSIGN);
			case T_ORASSIGN:
				parser.next();
				right = readAssignExpression(parser, options);
				return new ParsedExpressionOpAssign(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.ORASSIGN);
			case T_ANDASSIGN:
				parser.next();
				right = readAssignExpression(parser, options);
				return new ParsedExpressionOpAssign(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.ANDASSIGN);
			case T_XORASSIGN:
				parser.next();
				right = readAssignExpression(parser, options);
				return new ParsedExpressionOpAssign(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.XORASSIGN);
			case T_SHLASSIGN:
				parser.next();
				right = readAssignExpression(parser, options);
				return new ParsedExpressionOpAssign(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.SHLASSIGN);
			case T_SHRASSIGN:
				parser.next();
				right = readAssignExpression(parser, options);
				return new ParsedExpressionOpAssign(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.SHRASSIGN);
			case T_USHRASSIGN:
				parser.next();
				right = readAssignExpression(parser, options);
				return new ParsedExpressionOpAssign(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.USHRASSIGN);
		}

		return left;
	}

	private static ParsedExpression readConditionalExpression(CodePosition position, ZSTokenParser parser, ParsingOptions options) throws ParseException {
		ParsedExpression left = readOrOrExpression(position, parser, options);

		if (parser.optional(T_QUEST) != null) {
			ParsedExpression onIf = readOrOrExpression(parser.getPosition(), parser, options);
			parser.required(T_COLON, ": expected");
			ParsedExpression onElse = readConditionalExpression(parser.getPosition(), parser, options);
			return new ParsedExpressionConditional(position.until(parser.getPositionBeforeWhitespace()), left, onIf, onElse);
		}

		return left;
	}

	private static ParsedExpression readOrOrExpression(CodePosition position, ZSTokenParser parser, ParsingOptions options) throws ParseException {
		ParsedExpression left = readAndAndExpression(position, parser, options);

		while (parser.optional(T_OROR) != null) {
			ParsedExpression right = readAndAndExpression(parser.getPosition(), parser, options);
			left = new ParsedExpressionOrOr(position.until(parser.getPositionBeforeWhitespace()), left, right);
		}

		while (parser.optional(T_COALESCE) != null) {
			ParsedExpression right = readAndAndExpression(parser.getPosition(), parser, options);
			left = new ParsedExpressionCoalesce(position.until(parser.getPositionBeforeWhitespace()), left, right);
		}

		return left;
	}

	private static ParsedExpression readAndAndExpression(CodePosition position, ZSTokenParser parser, ParsingOptions options) throws ParseException {
		ParsedExpression left = readOrExpression(position, parser, options);

		while (parser.optional(T_ANDAND) != null) {
			ParsedExpression right = readOrExpression(parser.getPosition(), parser, options);
			left = new ParsedExpressionAndAnd(position.until(parser.getPositionBeforeWhitespace()), left, right);
		}
		return left;
	}

	private static ParsedExpression readOrExpression(CodePosition position, ZSTokenParser parser, ParsingOptions options) throws ParseException {
		ParsedExpression left = readXorExpression(position, parser, options);

		while (parser.optional(T_OR) != null) {
			ParsedExpression right = readXorExpression(parser.getPosition(), parser, options);
			left = new ParsedExpressionBinary(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.OR);
		}
		return left;
	}

	private static ParsedExpression readXorExpression(CodePosition position, ZSTokenParser parser, ParsingOptions options) throws ParseException {
		ParsedExpression left = readAndExpression(position, parser, options);

		while (parser.optional(T_XOR) != null) {
			ParsedExpression right = readAndExpression(parser.getPosition(), parser, options);
			left = new ParsedExpressionBinary(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.XOR);
		}
		return left;
	}

	private static ParsedExpression readAndExpression(CodePosition position, ZSTokenParser parser, ParsingOptions options) throws ParseException {
		ParsedExpression left = readCompareExpression(position, parser, options);

		while (parser.optional(T_AND) != null) {
			ParsedExpression right = readCompareExpression(parser.getPosition(), parser, options);
			left = new ParsedExpressionBinary(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.AND);
		}
		return left;
	}

	private static ParsedExpression readCompareExpression(CodePosition position, ZSTokenParser parser, ParsingOptions options) throws ParseException {
		ParsedExpression left = readShiftExpression(position, parser, options);

		switch (parser.peek().getType()) {
			case T_EQUAL2: {
				parser.next();
				ParsedExpression right = readShiftExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionCompare(position.until(parser.getPositionBeforeWhitespace()), left, right, CompareType.EQ);
			}
			case T_EQUAL3: {
				parser.next();
				ParsedExpression right = readShiftExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionSame(position.until(parser.getPositionBeforeWhitespace()), left, right, false);
			}
			case T_NOTEQUAL: {
				parser.next();
				ParsedExpression right = readShiftExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionCompare(position.until(parser.getPositionBeforeWhitespace()), left, right, CompareType.NE);
			}
			case T_NOTEQUAL2: {
				parser.next();
				ParsedExpression right = readShiftExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionSame(position.until(parser.getPositionBeforeWhitespace()), left, right, true);
			}
			case T_LESS: {
				parser.next();
				ParsedExpression right = readShiftExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionCompare(position.until(parser.getPositionBeforeWhitespace()), left, right, CompareType.LT);
			}
			case T_LESSEQ: {
				parser.next();
				ParsedExpression right = readShiftExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionCompare(position.until(parser.getPositionBeforeWhitespace()), left, right, CompareType.LE);
			}
			case T_GREATER: {
				parser.next();
				ParsedExpression right = readShiftExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionCompare(position.until(parser.getPositionBeforeWhitespace()), left, right, CompareType.GT);
			}
			case T_GREATEREQ: {
				parser.next();
				ParsedExpression right = readShiftExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionCompare(position.until(parser.getPositionBeforeWhitespace()), left, right, CompareType.GE);
			}
			case K_IN: {
				parser.next();
				ParsedExpression right = readShiftExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionBinary(position.until(parser.getPositionBeforeWhitespace()), right, left, OperatorType.CONTAINS);
			}
			case K_IS: {
				parser.next();
				IParsedType type = IParsedType.parse(parser);
				return new ParsedExpressionIs(position.until(parser.getPositionBeforeWhitespace()), left, type);
			}
			case T_NOT: {
				parser.next();
				if (parser.optional(K_IN) != null) {
					ParsedExpression right = readShiftExpression(parser.getPosition(), parser, options);
					return new ParsedExpressionUnary(position.until(parser.getPositionBeforeWhitespace()), new ParsedExpressionBinary(position, right, left, OperatorType.CONTAINS), OperatorType.NOT);
				} else if (parser.optional(K_IS) != null) {
					IParsedType type = IParsedType.parse(parser);
					return new ParsedExpressionUnary(position.until(parser.getPositionBeforeWhitespace()), new ParsedExpressionIs(position, left, type), OperatorType.NOT);
				} else {
					throw new ParseException(position.until(parser.getPositionBeforeWhitespace()), "Expected in or is");
				}
			}
		}

		return left;
	}

	private static ParsedExpression readShiftExpression(CodePosition position, ZSTokenParser parser, ParsingOptions options) throws ParseException {
		ParsedExpression left = readAddExpression(position, parser, options);

		while (true) {
			if (parser.optional(T_SHL) != null) {
				ParsedExpression right = readAddExpression(parser.getPosition(), parser, options);
				left = new ParsedExpressionBinary(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.SHL);
			} else if (parser.optional(T_SHR) != null) {
				ParsedExpression right = readAddExpression(parser.getPosition(), parser, options);
				left = new ParsedExpressionBinary(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.SHR);
			} else if (parser.optional(T_USHR) != null) {
				ParsedExpression right = readAddExpression(parser.getPosition(), parser, options);
				left = new ParsedExpressionBinary(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.USHR);
			} else {
				break;
			}
		}

		return left;
	}

	private static ParsedExpression readAddExpression(CodePosition position, ZSTokenParser parser, ParsingOptions options) throws ParseException {
		ParsedExpression left = readMulExpression(position, parser, options);

		while (true) {
			if (parser.optional(T_ADD) != null) {
				ParsedExpression right = readMulExpression(parser.getPosition(), parser, options);
				left = new ParsedExpressionBinary(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.ADD);
			} else if (parser.optional(T_SUB) != null) {
				ParsedExpression right = readMulExpression(parser.getPosition(), parser, options);
				left = new ParsedExpressionBinary(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.SUB);
			} else if (parser.optional(T_CAT) != null) {
				ParsedExpression right = readMulExpression(parser.getPosition(), parser, options);
				left = new ParsedExpressionBinary(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.CAT);
			} else {
				break;
			}
		}
		return left;
	}

	private static ParsedExpression readMulExpression(CodePosition position, ZSTokenParser parser, ParsingOptions options) throws ParseException {
		ParsedExpression left = readUnaryExpression(position, parser, options);

		while (true) {
			if (parser.optional(T_MUL) != null) {
				ParsedExpression right = readUnaryExpression(parser.getPosition(), parser, options);
				left = new ParsedExpressionBinary(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.MUL);
			} else if (parser.optional(T_DIV) != null) {
				ParsedExpression right = readUnaryExpression(parser.getPosition(), parser, options);
				left = new ParsedExpressionBinary(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.DIV);
			} else if (parser.optional(T_MOD) != null) {
				ParsedExpression right = readUnaryExpression(parser.getPosition(), parser, options);
				left = new ParsedExpressionBinary(position.until(parser.getPositionBeforeWhitespace()), left, right, OperatorType.MOD);
			} else {
				break;
			}
		}

		return left;
	}

	private static ParsedExpression readUnaryExpression(CodePosition position, ZSTokenParser parser, ParsingOptions options) throws ParseException {
		final ParsedExpression value;
		switch (parser.peek().getType()) {
			case T_NOT:
				parser.next();
				value = readUnaryExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionUnary(
						position.until(parser.getPositionBeforeWhitespace()),
						value,
						OperatorType.NOT);
			case T_SUB:
				parser.next();
				value = readUnaryExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionUnary(
						position.until(parser.getPositionBeforeWhitespace()),
						value,
						OperatorType.NEG);
			case T_CAT:
				parser.next();
				value = readUnaryExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionUnary(
						position.until(parser.getPositionBeforeWhitespace()),
						value,
						OperatorType.CAT);
			case T_INCREMENT:
				parser.next();
				value = readUnaryExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionUnary(
						position.until(parser.getPositionBeforeWhitespace()),
						value,
						OperatorType.INCREMENT);
			case T_DECREMENT:
				parser.next();
				value = readUnaryExpression(parser.getPosition(), parser, options);
				return new ParsedExpressionUnary(
						position.until(parser.getPositionBeforeWhitespace()),
						value,
						OperatorType.DECREMENT);
			case K_TRY:
				parser.next();
				value = readUnaryExpression(parser.getPosition(), parser, options);

				if (parser.optional(T_QUEST) != null) {
					// try? - attempts the specified operation returning T and throwing E. The result is converted to Result<T, E> depending on success or failure
					return new ParsedTryConvertExpression(position.until(parser.getPositionBeforeWhitespace()), value);
				} else if (parser.optional(T_NOT) != null) {
					// try! - attempts the specified operation
					// a) if the operation throws E and returns T
					//		- if the operation succeeds, returns T
					//		- if the operation thows and the function throws, will attempt to convert E to the thrown type and throw it
					//		- if the operation throws and the function returns Result<?, X>, will attempt to convert E to X and return a failure
					// b) if the operation returns Result<T, E>
					//		- if the operation succeeds, returns T
					//		- if the operation fails and the function throws, will attempt to convert E to the thrown type and throw it
					//		- if the operation fails and the function returns Result<?, X>, will attempt to convert E to X and return a failure
					// try! can thus be used to convert from exception-based to result-based and vice versa
					return new ParsedTryRethrowExpression(position.until(parser.getPositionBeforeWhitespace()), value);
				}
			default:
				return readPostfixExpression(position, parser, options);
		}
	}

	private static ParsedExpression readPostfixExpression(CodePosition position, ZSTokenParser parser, ParsingOptions options) throws ParseException {
		ParsedExpression base = readPrimaryExpression(position, parser, options);

		while (true) {
			if (parser.optional(T_DOT) != null) {
				ZSToken indexString = parser.optional(T_IDENTIFIER);
				if (indexString != null) {
					List<IParsedType> genericParameters = IParsedType.parseTypeArguments(parser);
					base = new ParsedExpressionMember(position.until(parser.getPositionBeforeWhitespace()), base, indexString.content, genericParameters);
				} else if (parser.optional(T_DOLLAR) != null) {
					base = new ParsedExpressionOuter(position.until(parser.getPositionBeforeWhitespace()), base);
				} else {
					ZSToken indexString2 = parser.optional(T_STRING_SQ);
					if (indexString2 == null)
						indexString2 = parser.optional(T_STRING_DQ);

					if (indexString2 != null) {
						// TODO: handle this properly
						base = new ParsedExpressionMember(position.until(parser.getPositionBeforeWhitespace()), base, unescape(indexString2.content).orElse("INVALID STRING"), Collections.emptyList());
					} else {
						position = parser.getPosition();
						ZSToken last = parser.next();
						throw new ParseException(position.until(parser.getPositionBeforeWhitespace()), "Invalid expression, last token: " + last.content);
					}
				}
			} else if (parser.optional(T_DOT2) != null) {
				ParsedExpression to = readAssignExpression(parser, options);
				return new ParsedExpressionRange(position.until(parser.getPositionBeforeWhitespace()), base, to);
			} else if (parser.optional(T_SQOPEN) != null) {
				List<ParsedExpression> indexes = new ArrayList<>();
				do {
					indexes.add(readAssignExpression(parser, options));
				} while (parser.optional(ZSTokenType.T_COMMA) != null);
				parser.required(T_SQCLOSE, "] expected");
				base = new ParsedExpressionIndex(position.until(parser.getPositionBeforeWhitespace()), base, indexes);
			} else if (parser.isNext(T_BROPEN)) {
				final ParsedCallArguments arguments = ParsedCallArguments.parse(parser);
				base = new ParsedExpressionCall(position.until(parser.getPositionBeforeWhitespace()), base, arguments);
			} else if (parser.optional(K_AS) != null) {
				boolean optional = parser.optional(T_QUEST) != null;
				IParsedType type = IParsedType.parse(parser);
				base = new ParsedExpressionCast(position.until(parser.getPositionBeforeWhitespace()), base, type, optional);
			} else if (parser.optional(T_INCREMENT) != null) {
				base = new ParsedExpressionPostCall(position.until(parser.getPositionBeforeWhitespace()), base, OperatorType.INCREMENT);
			} else if (parser.optional(T_DECREMENT) != null) {
				base = new ParsedExpressionPostCall(position.until(parser.getPositionBeforeWhitespace()), base, OperatorType.DECREMENT);
			} else if (options.allowLambda && parser.optional(T_LAMBDA) != null) {
				ParsedFunctionBody body = ParsedStatement.parseLambdaBody(parser, true);
				base = new ParsedExpressionFunction(position.until(parser.getPositionBeforeWhitespace()), base.toLambdaHeader(), body);
			} else {
				break;
			}
		}

		return base;
	}

	private static ParsedExpression readPrimaryExpression(CodePosition position, ZSTokenParser parser, ParsingOptions options) throws ParseException {
		switch (parser.peek().getType()) {
			case T_INT:
				final String intContent = parser.next().content;
				return new ParsedExpressionInt(position.until(parser.getPositionBeforeWhitespace()), intContent);
			case T_PREFIXED_INT:
				final String prefixedIntContent = parser.next().content;
				return ParsedExpressionInt.parsePrefixed(position, prefixedIntContent);
			case T_FLOAT:
				final String floatContent = parser.next().content;
				return new ParsedExpressionFloat(
						position.until(parser.getPositionBeforeWhitespace()),
						floatContent);
			case T_STRING_SQ:
			case T_STRING_DQ: {
				String quoted = parser.next().content;
				return new ParsedExpressionString(
						position.until(parser.getPositionBeforeWhitespace()),
						StringExpansion.unescape(quoted).orElse(error -> "INVALID_STRING"),
						quoted.charAt(0) == '\'');
			}
			case T_IDENTIFIER: {
				String name = parser.next().content;
				if (name.startsWith("@"))
					name = name.substring(1);

				List<IParsedType> genericParameters = IParsedType.parseTypeArguments(parser);
				return new ParsedExpressionVariable(position.until(parser.getPositionBeforeWhitespace()), name, genericParameters);
			}
			case T_LOCAL_IDENTIFIER: {
				String name = parser.next().content.substring(1);
				return new ParsedLocalVariableExpression(position.until(parser.getPositionBeforeWhitespace()), name);
			}
			case K_THIS:
				parser.next();
				return new ParsedExpressionThis(position.until(parser.getPositionBeforeWhitespace()));
			case K_SUPER:
				parser.next();
				return new ParsedExpressionSuper(position.until(parser.getPositionBeforeWhitespace()));
			case T_DOLLAR:
				parser.next();
				return new ParsedDollarExpression(position.until(parser.getPositionBeforeWhitespace()));
			case T_SQOPEN: {
				parser.next();
				List<ParsedExpression> contents = new ArrayList<>();
				if (parser.optional(T_SQCLOSE) == null) {
					while (parser.optional(T_SQCLOSE) == null) {
						contents.add(readAssignExpression(parser, options));
						if (parser.optional(T_COMMA) == null) {
							parser.required(T_SQCLOSE, "] or , expected");
							break;
						}
					}
				}
				return new ParsedExpressionArray(position.until(parser.getPositionBeforeWhitespace()), contents);
			}
			case T_AOPEN: {
				parser.next();

				List<ParsedExpression> keys = new ArrayList<>();
				List<ParsedExpression> values = new ArrayList<>();
				while (parser.optional(T_ACLOSE) == null) {
					ParsedExpression expression = readAssignExpression(parser, options);
					if (parser.optional(T_COLON) == null) {
						keys.add(null);
						values.add(expression);
					} else {
						keys.add(expression);
						values.add(readAssignExpression(parser, options));
					}

					if (parser.optional(T_COMMA) == null) {
						parser.required(T_ACLOSE, "} or , expected");
						break;
					}
				}
				return new ParsedExpressionMap(position.until(parser.getPositionBeforeWhitespace()), keys, values);
			}
			case K_TRUE:
				parser.next();
				return new ParsedExpressionBool(position.until(parser.getPositionBeforeWhitespace()), true);
			case K_FALSE:
				parser.next();
				return new ParsedExpressionBool(position.until(parser.getPositionBeforeWhitespace()), false);
			case K_NULL:
				parser.next();
				return new ParsedExpressionNull(position.until(parser.getPositionBeforeWhitespace()));
			case T_BROPEN: {
				parser.next();
				List<ParsedExpression> expressions = new ArrayList<>();
				do {
					if (parser.peek().type == T_BRCLOSE) {
						break;
					}
					expressions.add(readAssignExpression(parser, options));
				} while (parser.optional(ZSTokenType.T_COMMA) != null);
				parser.required(T_BRCLOSE, ") expected");
				return new ParsedExpressionBracket(position.until(parser.getPositionBeforeWhitespace()), expressions);
			}
			case K_NEW: {
				parser.next();
				IParsedType type = IParsedType.parse(parser);
				ParsedCallArguments newArguments = ParsedCallArguments.NONE;
				if (parser.isNext(ZSTokenType.T_BROPEN) || parser.isNext(ZSTokenType.T_LESS))
					newArguments = ParsedCallArguments.parse(parser);

				return new ParsedNewExpression(position.until(parser.getPositionBeforeWhitespace()), type, newArguments);
			}
			case K_THROW: {
				parser.next();
				ParsedExpression value = parse(parser);
				return new ParsedThrowExpression(position.until(parser.getPositionBeforeWhitespace()), value);
			}
			case K_PANIC: {
				parser.next();
				ParsedExpression value = parse(parser);
				return new ParsedPanicExpression(position.until(parser.getPositionBeforeWhitespace()), value);
			}
			case K_MATCH: {
				parser.next();
				ParsedExpression source = parse(parser);
				parser.required(T_AOPEN, "{ expected");

				List<ParsedMatchExpression.Case> cases = new ArrayList<>();
				while (parser.optional(T_ACLOSE) == null) {
					ParsedExpression key = null;
					if (parser.optional(K_DEFAULT) == null)
						key = parse(parser, new ParsingOptions(false));

					parser.required(T_LAMBDA, "=> expected");
					ParsedExpression value = parse(parser);
					cases.add(new ParsedMatchExpression.Case(key, value));

					if (parser.optional(T_COMMA) == null)
						break;
				}
				parser.required(T_ACLOSE, "} expected");
				return new ParsedMatchExpression(position.until(parser.getPositionBeforeWhitespace()), source, cases);
			}
			case T_LESS:// bracket expression
				parser.next();
				if (parser.bracketParser == null)
					throw new ParseException(position, "Bracket expression detected but no bracket parser present");

				return parser.bracketParser.parse(position.until(parser.getPositionBeforeWhitespace()), parser);
			default: {
				IParsedType type = IParsedType.parse(parser);
				return new ParsedTypeExpression(position.until(parser.getPositionBeforeWhitespace()), type);
			}
		}
	}

	/**
	 * Compiles the given parsed expression to a high-level expression or
	 * partial expression.
	 * <p>
	 * If the asType parameter is provided, the given type determines the output
	 * type of the expression. The output type of the expression MUST in that
	 * case be equal to the given type.
	 *
	 * @param scope
	 * @return
	 */
	public abstract IPartialExpression compile(ExpressionScope scope) throws CompileException;

	public Expression compileKey(ExpressionScope scope) throws CompileException {
		return compile(scope).eval();
	}

	public SwitchValue compileToSwitchValue(TypeID type, ExpressionScope scope) throws CompileException {
		throw new CompileException(position, CompileExceptionCode.INVALID_SWITCH_CASE, "Invalid switch case");
	}

	public ParsedFunctionHeader toLambdaHeader() throws ParseException {
		throw new ParseException(position, "Not a valid lambda header");
	}

	public ParsedFunctionParameter toLambdaParameter() throws ParseException {
		throw new ParseException(position, "Not a valid lambda parameter");
	}

	public boolean isCompatibleWith(BaseScope scope, TypeID type) {
		return true;
	}

	public abstract boolean hasStrongType();

	public static class ParsingOptions {
		public static final ParsingOptions DEFAULT = new ParsingOptions(true);

		public final boolean allowLambda;

		public ParsingOptions(boolean allowLambda) {
			this.allowLambda = allowLambda;
		}
	}
}
