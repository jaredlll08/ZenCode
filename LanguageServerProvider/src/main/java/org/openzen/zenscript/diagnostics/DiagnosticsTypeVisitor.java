package org.openzen.zenscript.diagnostics;

import org.eclipse.lsp4j.Diagnostic;
import org.openzen.zenscript.OpenFileInfo;
import org.openzen.zenscript.codemodel.type.*;

import java.util.List;

public class DiagnosticsTypeVisitor implements TypeVisitor<Void> {
	private final List<Diagnostic> diagnostics;

	public DiagnosticsTypeVisitor(List<Diagnostic> diagnostics) {
		this.diagnostics = diagnostics;
	}

	@Override
	public Void visitBasic(BasicTypeID basic) {
		return null;
	}

	@Override
	public Void visitArray(ArrayTypeID array) {
		return null;
	}

	@Override
	public Void visitAssoc(AssocTypeID assoc) {
		return null;
	}

	@Override
	public Void visitGenericMap(GenericMapTypeID map) {
		return null;
	}

	@Override
	public Void visitIterator(IteratorTypeID iterator) {
		return null;
	}

	@Override
	public Void visitFunction(FunctionTypeID function) {
		return null;
	}

	@Override
	public Void visitDefinition(DefinitionTypeID definition) {
		return null;
	}

	@Override
	public Void visitGeneric(GenericTypeID generic) {
		return null;
	}

	@Override
	public Void visitRange(RangeTypeID range) {
		return null;
	}

	@Override
	public Void visitOptional(OptionalTypeID type) {
		return null;
	}

	@Override
	public Void visitInvalid(InvalidTypeID type) {
		this.diagnostics.add(new Diagnostic(OpenFileInfo.codePositionToRange(type.position), type.error.toString()));
		return null;
	}
}
