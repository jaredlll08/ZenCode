/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.parser.expression;

import java.util.Collections;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zencode.shared.CompileException;
import org.openzen.zencode.shared.CompileExceptionCode;
import org.openzen.zenscript.codemodel.expression.CallArguments;
import org.openzen.zenscript.codemodel.expression.ConstantByteExpression;
import org.openzen.zenscript.codemodel.expression.ConstantCharExpression;
import org.openzen.zenscript.codemodel.expression.ConstantIntExpression;
import org.openzen.zenscript.codemodel.expression.ConstantLongExpression;
import org.openzen.zenscript.codemodel.expression.ConstantSByteExpression;
import org.openzen.zenscript.codemodel.expression.ConstantShortExpression;
import org.openzen.zenscript.codemodel.expression.ConstantUIntExpression;
import org.openzen.zenscript.codemodel.expression.ConstantULongExpression;
import org.openzen.zenscript.codemodel.expression.ConstantUShortExpression;
import org.openzen.zenscript.codemodel.expression.ConstantUSizeExpression;
import org.openzen.zenscript.codemodel.expression.Expression;
import org.openzen.zenscript.codemodel.expression.switchvalue.IntSwitchValue;
import org.openzen.zenscript.codemodel.expression.switchvalue.SwitchValue;
import org.openzen.zenscript.codemodel.member.ref.FunctionalMemberRef;
import org.openzen.zenscript.codemodel.type.BasicTypeID;
import org.openzen.zenscript.codemodel.type.ITypeID;
import org.openzen.zenscript.codemodel.scope.ExpressionScope;
import org.openzen.zenscript.codemodel.type.member.TypeMembers;

/**
 *
 * @author Hoofdgebruiker
 */
public class ParsedExpressionInt extends ParsedExpression {
	public static ParsedExpressionInt parsePrefixed(CodePosition position, String value) {
		boolean negative = value.startsWith("-");
		if (negative)
			value = value.substring(1);
		
		String suffix = "";
		if (value.endsWith("u") || value.endsWith("l") || value.endsWith("U") || value.endsWith("L")) {
			suffix = value.substring(value.length() - 1);
			value = value.substring(0, value.length() - 1);
		} else if (value.endsWith("ul") || value.endsWith("UL")) {
			suffix = value.substring(value.length() - 2);
			value = value.substring(0, value.length() - 2);
		}
		
		value = value.toLowerCase();
		
		long parsed = 0;
		if (value.startsWith("0x")) {
			for (char c : value.substring(2).toCharArray()) {
				if (c >= '0' && c <= '9')
					parsed = parsed * 16 + (c - '0');
				else if (c >= 'a' && c <= 'f')
					parsed = parsed * 16 + 10 + (c - 'a');
				else if (c != '_')
					throw new NumberFormatException("Invalid number: " + value);
			}
		} else if (value.startsWith("0b")) {
			for (char c : value.substring(2).toCharArray()) {
				if (c == '0')
					parsed = parsed * 2;
				else if (c == '1')
					parsed = parsed * 2 + 1;
				else if (c != '_')
					throw new NumberFormatException("Invalid number: " + value);
			}
		} else if (value.startsWith("0o")) {
			for (char c : value.substring(2).toCharArray()) {
				if (c >= '0' && c <= '7')
					parsed = parsed * 8 + c - '0';
				else if (c != '_')
					throw new NumberFormatException("Invalid number: " + value);
			}
		} else {
			throw new NumberFormatException("Invalid number: " + value);
		}
		
		return new ParsedExpressionInt(position, negative, negative ? -parsed : parsed, suffix);
	}
	
	public final boolean negative;
	public final long value;
	public final String suffix;
	
	public ParsedExpressionInt(CodePosition position, String value) {
		super(position);
		
		int split = value.length();
		while (isLetter(value.charAt(split - 1)))
			split--;
		
		negative = value.charAt(0) == '-';
		this.value = Long.parseLong(value.substring(0, split));
		suffix = value.substring(split);
	}
	
	private ParsedExpressionInt(CodePosition position, boolean negative, long value, String suffix) {
		super(position);
		
		this.negative = negative;
		this.value = value;
		this.suffix = suffix;
	}
	
	@Override
	public Expression compile(ExpressionScope scope) {
		if (suffix.equals("L") || suffix.equals("l"))
			return new ConstantLongExpression(position, value);
		if (suffix.equals("UL") || suffix.equals("ul"))
			return new ConstantULongExpression(position, value);
		if (suffix.equals("U") || suffix.equals("u"))
			return new ConstantUIntExpression(position, (int)value);
		
		for (ITypeID hint : scope.hints) {
			if (suffix.isEmpty() && (hint instanceof BasicTypeID)) {
				switch ((BasicTypeID) hint) {
					case SBYTE:
						return new ConstantSByteExpression(position, (byte) value);
					case BYTE:
						if (negative)
							break;
						
						return new ConstantByteExpression(position, (int)(value & 0xFF));
					case SHORT:
						return new ConstantShortExpression(position, (short) value);
					case USHORT:
						if (negative)
							break;
						
						return new ConstantUShortExpression(position, (int)(value & 0xFFFF));
					case INT:
						return new ConstantIntExpression(position, (int) value);
					case UINT:
						if (negative)
							break;
						
						return new ConstantUIntExpression(position, (int) value);
					case LONG:
						return new ConstantLongExpression(position, value);
					case ULONG:
						if (negative)
							break;
						
						return new ConstantULongExpression(position, value);
					case USIZE:
						if (negative)
							break;
						
						return new ConstantUSizeExpression(position, value);
					case CHAR:
						if (negative)
							break;
						
						return new ConstantCharExpression(position, (char) value);
					default:
				}
			} else if (!suffix.isEmpty()) {
				TypeMembers members = scope.getTypeMembers(hint);
				FunctionalMemberRef method = members.getOrCreateGroup(suffix, true).getStaticMethod(1, hint);
				if (method != null) {
					ParsedCallArguments parsedArguments = new ParsedCallArguments(Collections.emptyList(), Collections.singletonList(new ParsedExpressionInt(position, negative, value, "")));
					CallArguments arguments = parsedArguments.compileCall(position, scope, ITypeID.NONE, method.getHeader());
					method.callStatic(position, hint, method.getHeader(), arguments, scope);
				}
			}
		}
		
		if (suffix.isEmpty()) {
			if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE)
				return new ConstantIntExpression(position, (int) value);
			else
				return new ConstantLongExpression(position, value);
		} else {
			throw new CompileException(position, CompileExceptionCode.INVALID_SUFFIX, "Invalid suffix: " + suffix);
		}
	}
	
	@Override
	public SwitchValue compileToSwitchValue(ITypeID type, ExpressionScope scope) {
		if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
			throw new CompileException(position, CompileExceptionCode.INVALID_SWITCH_CASE, "value is too large for a switch case");
		
		return new IntSwitchValue((int) value);
	}

	@Override
	public boolean hasStrongType() {
		return false;
	}
	
	private static boolean isLetter(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
	}
}
