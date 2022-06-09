package org.openzen.zenscript.codemodel.compilation;

import org.openzen.zenscript.codemodel.FunctionHeader;
import org.openzen.zenscript.codemodel.type.TypeID;

import java.util.Optional;

public class CompilableLambdaHeader {
	private final TypeID returnType;
	private final Parameter[] parameters;

	public CompilableLambdaHeader(TypeID returnType, Parameter... parameters) {
		this.returnType = returnType;
		this.parameters = parameters;
	}

	public FunctionHeader compile(TypeBuilder types) {

	}

	public static class Parameter {
		private final String name;
		private final TypeID type;

		public Parameter(String name, TypeID type) {
			this.name = name;
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public Optional<TypeID> getType() {
			return Optional.ofNullable(type);
		}
	}
}
