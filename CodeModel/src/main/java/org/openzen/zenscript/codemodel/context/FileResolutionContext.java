/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.codemodel.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.HighLevelDefinition;
import org.openzen.zenscript.codemodel.annotations.AnnotationDefinition;
import org.openzen.zenscript.codemodel.type.GenericName;
import org.openzen.zenscript.codemodel.type.GlobalTypeRegistry;
import org.openzen.zenscript.codemodel.type.ITypeID;
import org.openzen.zenscript.codemodel.type.storage.StorageTag;

/**
 *
 * @author Hoofdgebruiker
 */
public class FileResolutionContext implements TypeResolutionContext {
	private final ModuleTypeResolutionContext module;
	private final CompilingPackage modulePackage;
	private final Map<String, HighLevelDefinition> imports = new HashMap<>();
	
	public FileResolutionContext(ModuleTypeResolutionContext module, CompilingPackage modulePackage) {
		this.module = module;
		this.modulePackage = modulePackage;
	}
	
	public void addImport(String name, HighLevelDefinition definition) {
		imports.put(name, definition);
	}

	@Override
	public GlobalTypeRegistry getTypeRegistry() {
		return module.getTypeRegistry();
	}

	@Override
	public AnnotationDefinition getAnnotation(String name) {
		return module.getAnnotation(name);
	}

	@Override
	public ITypeID getType(CodePosition position, List<GenericName> name, StorageTag storage) {
		if (imports.containsKey(name.get(0).name)) {
			return GenericName.getInnerType(
					getTypeRegistry(),
					getTypeRegistry().getForDefinition(imports.get(name.get(0).name), null, name.get(0).arguments),
					name,
					1,
					storage);
		}
		
		ITypeID moduleType = modulePackage.getType(this, name);
		if (moduleType != null)
			return moduleType;
		
		return module.getType(position, name, storage);
	}
	
	@Override
	public StorageTag getStorageTag(CodePosition position, String name, String[] arguments) {
		return module.getStorageTag(position, name, arguments);
	}

	@Override
	public ITypeID getThisType() {
		return null;
	}
}
