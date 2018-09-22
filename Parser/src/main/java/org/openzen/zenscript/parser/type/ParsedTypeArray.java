/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.parser.type;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.context.TypeResolutionContext;
import org.openzen.zenscript.codemodel.type.GlobalTypeRegistry;
import org.openzen.zenscript.codemodel.type.ITypeID;
import org.openzen.zenscript.codemodel.type.ModifiedTypeID;
import org.openzen.zenscript.codemodel.type.storage.StorageTag;

/**
 *
 * @author Hoofdgebruiker
 */
public class ParsedTypeArray implements IParsedType {
	private final CodePosition position;
	public final IParsedType baseType;
	public final int dimension;
	public final int modifiers;
	public final ParsedStorageTag storage;
	
	public ParsedTypeArray(CodePosition position, IParsedType baseType, int dimension, ParsedStorageTag storage) {
		this.position = position;
		this.baseType = baseType;
		this.dimension = dimension;
		this.modifiers = 0;
		this.storage = storage;
	}

	private ParsedTypeArray(CodePosition position, IParsedType baseType, int dimension, int modifiers, ParsedStorageTag storage) {
		this.position = position;
		this.baseType = baseType;
		this.dimension = dimension;
		this.modifiers = modifiers;
		this.storage = storage;
	}
	
	@Override
	public IParsedType withOptional() {
		return new ParsedTypeArray(position, baseType, dimension, modifiers | ModifiedTypeID.MODIFIER_OPTIONAL, storage);
	}

	@Override
	public IParsedType withModifiers(int modifiers) {
		return new ParsedTypeArray(position, baseType, dimension, this.modifiers | modifiers, storage);
	}

	@Override
	public ITypeID compile(TypeResolutionContext context) {
		ITypeID baseType = this.baseType.compile(context);
		StorageTag storage = this.storage.resolve(position, context);
		GlobalTypeRegistry registry = context.getTypeRegistry();
		return registry.getModified(modifiers, registry.getArray(baseType, dimension, storage));
	}
}
