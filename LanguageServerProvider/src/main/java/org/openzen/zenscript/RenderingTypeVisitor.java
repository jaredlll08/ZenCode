package org.openzen.zenscript;

import org.openzen.zenscript.codemodel.FunctionHeader;
import org.openzen.zenscript.codemodel.generic.TypeParameter;
import org.openzen.zenscript.codemodel.type.*;
import org.openzen.zenscript.formatter.FormattingUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class RenderingTypeVisitor implements TypeVisitor<String> {

	@Override
	public String visitBasic(BasicTypeID basic) {
		return basic.name;
	}

	@Override
	public String visitArray(ArrayTypeID array) {
		return array.elementType.accept(this) + "[]";
	}

	@Override
	public String visitAssoc(AssocTypeID assoc) {

		return assoc.valueType.accept(this) + "[" + assoc.keyType.accept(this) + "]";
	}

	@Override
	public String visitGenericMap(GenericMapTypeID map) {
		StringBuilder result = new StringBuilder();
		result.append(map.value.accept(this));
		result.append("[<");
		//TODO
//		FormattingUtils.formatTypeParameters(result, new TypeParameter[]{map.key}, this);
		result.append("]>");
		return result.toString();
	}

	@Override
	public String visitIterator(IteratorTypeID iterator) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String visitFunction(FunctionTypeID function) {
		return renderFunctionHeader(function.header);
	}

	public static String renderFunctionHeader(FunctionHeader header) {
		String collect = Arrays.stream(header.parameters).map(functionParameter -> functionParameter.name + ": " + functionParameter.type.accept(new RenderingTypeVisitor())).collect(Collectors.joining(", ", "(", ")"));

		return collect + ": " + header.getReturnType().accept(new RenderingTypeVisitor());
	}

	@Override
	public String visitDefinition(DefinitionTypeID definition) {
		String importedName = definition.definition.getName();//importer.importDefinition(definition.definition);
		if (definition.typeArguments == null || definition.typeArguments.length == 0)
			return importedName;

		return importedName +
				Arrays.stream(definition.typeArguments)
						.map(typeID -> typeID.accept(this))
						.collect(Collectors.joining(", ", "<", ">"));
	}

	@Override
	public String visitGeneric(GenericTypeID generic) {
		return generic.parameter.name;
	}

	@Override
	public String visitRange(RangeTypeID range) {
		return range.baseType + " .. " + range.baseType;
	}

	@Override
	public String visitOptional(OptionalTypeID type) {
		return type.baseType.accept(this) + "?";
	}
}
