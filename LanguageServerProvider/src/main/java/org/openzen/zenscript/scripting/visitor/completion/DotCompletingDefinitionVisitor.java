package org.openzen.zenscript.scripting.visitor.completion;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.openzen.zenscript.codemodel.HighLevelDefinition;
import org.openzen.zenscript.codemodel.definition.*;
import org.openzen.zenscript.codemodel.member.EnumConstantMember;
import org.openzen.zenscript.codemodel.member.IDefinitionMember;

import java.util.ArrayList;
import java.util.List;

public class DotCompletingDefinitionVisitor implements DefinitionVisitor<List<CompletionItem>> {

	public List<CompletionItem> visitBase(HighLevelDefinition definition) {
		ArrayList<CompletionItem> completionItems = new ArrayList<>();
		for (IDefinitionMember member : definition.members) {
			member.accept(new CompletionMemberVisitor()).ifPresent(completionItems::add);
		}
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitClass(ClassDefinition definition) {
		return visitBase(definition);
	}

	@Override
	public List<CompletionItem> visitInterface(InterfaceDefinition definition) {
		return visitBase(definition);
	}

	@Override
	public List<CompletionItem> visitEnum(EnumDefinition definition) {

		List<CompletionItem> completionItems = visitBase(definition);
		for (EnumConstantMember enumConstant : definition.enumConstants) {
			CompletionItem completionItem = new CompletionItem();
			completionItem.setKind(CompletionItemKind.EnumMember);
			completionItem.setLabel(enumConstant.name);
			completionItem.setDocumentation(enumConstant.getPosition().toLongString());
			completionItems.add(completionItem);
		}
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitStruct(StructDefinition definition) {
		List<CompletionItem> completionItems = visitBase(definition);

		return completionItems;
	}

	@Override
	public List<CompletionItem> visitFunction(FunctionDefinition definition) {
		List<CompletionItem> completionItems = visitBase(definition);
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitExpansion(ExpansionDefinition definition) {
		List<CompletionItem> completionItems = visitBase(definition);
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitAlias(AliasDefinition definition) {
		List<CompletionItem> completionItems = visitBase(definition);
		return completionItems;
	}

	@Override
	public List<CompletionItem> visitVariant(VariantDefinition variant) {
		List<CompletionItem> completionItems = visitBase(variant);
		for (VariantDefinition.Option option : variant.options) {
			CompletionItem completionItem = new CompletionItem();
			completionItem.setKind(CompletionItemKind.Value);
			completionItem.setLabel(option.name);
			completionItem.setDocumentation(option.position.toLongString());
			completionItems.add(completionItem);
		}
		return completionItems;
	}
}
