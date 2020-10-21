/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.formatter;

import org.openzen.zenscript.formattershared.Importer;
import org.openzen.zenscript.codemodel.generic.GenericParameterBoundVisitor;
import org.openzen.zenscript.codemodel.generic.ParameterSuperBound;
import org.openzen.zenscript.codemodel.generic.ParameterTypeBound;
import org.openzen.zenscript.codemodel.generic.TypeParameter;
import org.openzen.zenscript.codemodel.type.ArrayTypeID;
import org.openzen.zenscript.codemodel.type.AssocTypeID;
import org.openzen.zenscript.codemodel.type.BasicTypeID;
import org.openzen.zenscript.codemodel.type.OptionalTypeID;
import org.openzen.zenscript.codemodel.type.DefinitionTypeID;
import org.openzen.zenscript.codemodel.type.FunctionTypeID;
import org.openzen.zenscript.codemodel.type.GenericMapTypeID;
import org.openzen.zenscript.codemodel.type.GenericTypeID;
import org.openzen.zenscript.codemodel.type.IteratorTypeID;
import org.openzen.zenscript.codemodel.type.RangeTypeID;
import org.openzen.zenscript.codemodel.type.StoredType;
import org.openzen.zenscript.codemodel.type.TypeID;
import stdlib.Chars;
import org.openzen.zenscript.codemodel.type.TypeVisitor;

/**
 *
 * @author Hoofdgebruiker
 */
public class TypeFormatter implements TypeVisitor<String>, GenericParameterBoundVisitor<String> {
	private final ScriptFormattingSettings settings;
	private final Importer importer;
	
	public TypeFormatter(ScriptFormattingSettings settings, Importer importer) {
		this.settings = settings;
		this.importer = importer;
	}
	
	public String format(TypeID type) {
		return type.accept(this);
	}
	
	public String format(StoredType type) {
		return type.type.accept(this) + (type.getSpecifiedStorage() == null ? "" : "`" + type.getSpecifiedStorage().toString());
	}

	@Override
	public String visitBasic(BasicTypeID basic) {
		return basic.name;
	}

	@Override
	public String visitArray(ArrayTypeID array) {
		String element = format(array.elementType);
		String result;
		if (array.dimension == 1) {
			result = element + "[]";
		} else {
			result = element + "[" + Chars.times(',', array.dimension - 1) + "]";
		}
		return result;
	}

	@Override
	public String visitAssoc(AssocTypeID assoc) {
		return format(assoc.valueType) + "[" + format(assoc.keyType) + "]";
	}

	@Override
	public String visitIterator(IteratorTypeID iterator) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String visitFunction(FunctionTypeID function) {
		StringBuilder result = new StringBuilder();
		result.append("function");
		FormattingUtils.formatHeader(result, settings, function.header, this);
		return result.toString();
	}

	@Override
	public String visitDefinition(DefinitionTypeID definition) {
		String importedName = importer.importDefinition(definition.definition);
		if (definition.typeArguments == null)
			return importedName;
		
		StringBuilder result = new StringBuilder();
		result.append(importedName);
		result.append("<");
		int index = 0;
		for (TypeID typeParameter : definition.typeArguments) {
			if (index > 0)
				result.append(", ");
			
			result.append(format(typeParameter));
		}
		result.append(">");
		return result.toString();
	}

	@Override
	public String visitGeneric(GenericTypeID generic) {
		return generic.parameter.name;
	}

	@Override
	public String visitRange(RangeTypeID range) {
		return format(range.baseType) + " .. " + format(range.baseType);
	}

	@Override
	public String visitOptional(OptionalTypeID type) {
		return type.accept(this) + "?";
	}

	@Override
	public String visitSuper(ParameterSuperBound bound) {
		return "super " + bound.type.accept(this);
	}

	@Override
	public String visitType(ParameterTypeBound bound) {
		return bound.type.accept(this);
	}

	@Override
	public String visitGenericMap(GenericMapTypeID map) {
		StringBuilder result = new StringBuilder();
		result.append(format(map.value));
		result.append("[<");
		FormattingUtils.formatTypeParameters(result, new TypeParameter[] { map.key }, this);
		result.append("]>");
		return result.toString();
	}
}
