package org.openzen.zenscript.scripting.visitor.completion;

import org.eclipse.lsp4j.CompletionItem;
import org.openzen.zenscript.codemodel.type.*;

import java.util.ArrayList;
import java.util.List;

public class DotCompletionTypeVisitor implements TypeVisitor<List<CompletionItem>> {

	@Override
	public List<CompletionItem> visitBasic(BasicTypeID basic) {
		List<CompletionItem> completionItems = new ArrayList<>();
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitArray(ArrayTypeID array) {
		List<CompletionItem> completionItems = new ArrayList<>();
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitAssoc(AssocTypeID assoc) {
		List<CompletionItem> completionItems = new ArrayList<>();
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitGenericMap(GenericMapTypeID map) {
		List<CompletionItem> completionItems = new ArrayList<>();
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitIterator(IteratorTypeID iterator) {
		List<CompletionItem> completionItems = new ArrayList<>();
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitFunction(FunctionTypeID function) {
		List<CompletionItem> completionItems = new ArrayList<>();
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitDefinition(DefinitionTypeID definition) {
		List<CompletionItem> completionItems = new ArrayList<>();
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitGeneric(GenericTypeID generic) {
		List<CompletionItem> completionItems = new ArrayList<>();
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitRange(RangeTypeID range) {
		List<CompletionItem> completionItems = new ArrayList<>();
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitOptional(OptionalTypeID type) {
		List<CompletionItem> completionItems = new ArrayList<>();
		return completionItems;
	}
}
