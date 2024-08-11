package org.openzen.zenscript.formatter;

import org.openzen.zencode.shared.StringExpansion;
import org.openzen.zenscript.codemodel.expression.switchvalue.*;

public class SwitchValueFormatter implements SwitchValueVisitor<String> {
	private final ScriptFormattingSettings settings;

	public SwitchValueFormatter(ScriptFormattingSettings settings) {
		this.settings = settings;
	}

	@Override
	public String acceptInt(IntSwitchValue value) {
		return Integer.toString(value.value);
	}

	@Override
	public String acceptChar(CharSwitchValue value) {
		return StringExpansion.escape(new String(new char[]{value.value}), '\'', true);
	}

	@Override
	public String acceptString(StringSwitchValue value) {
		return StringExpansion.escape(value.value, settings.useSingleQuotesForStrings ? '\'' : '"', true);
	}

	@Override
	public String acceptEnumConstant(EnumConstantSwitchValue value) {
		return value.constant.name;
	}

	@Override
	public String acceptVariantOption(VariantOptionSwitchValue value) {
		StringBuilder result = new StringBuilder();
		result.append(value.option.getName());
		result.append("(");
		for (int i = 0; i < value.parameters.length; i++) {
			if (i > 0)
				result.append(", ");
			result.append(value.parameters[i]);
		}
		result.append(")");
		return result.toString();
	}

	@Override
	public String acceptError(ErrorSwitchValue value) {
		return "";
	}
}
