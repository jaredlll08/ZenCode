package org.openzen.zenscript.codemodel.compilation;

import org.openzen.zenscript.codemodel.FunctionHeader;
import org.openzen.zenscript.codemodel.identifiers.MethodSymbol;
import org.openzen.zenscript.codemodel.identifiers.instances.MethodInstance;

import java.util.Optional;

public interface AnyMethod {
	FunctionHeader getHeader();

	Optional<MethodInstance> asMethod();
}
