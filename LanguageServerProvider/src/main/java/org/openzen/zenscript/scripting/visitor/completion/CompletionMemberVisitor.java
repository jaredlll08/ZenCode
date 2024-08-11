package org.openzen.zenscript.scripting.visitor.completion;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.openzen.zenscript.RenderingTypeVisitor;
import org.openzen.zenscript.codemodel.member.*;

import java.util.Optional;

public class CompletionMemberVisitor implements MemberVisitor<Optional<CompletionItem>> {

	@Override
	public Optional<CompletionItem> visitField(FieldMember member) {
		CompletionItem completionItem = new CompletionItem();
		completionItem.setKind(CompletionItemKind.Field);
		completionItem.setLabel(member.getName());
		completionItem.setDocumentation(member.getPosition().toLongString());
		return Optional.of(completionItem);
	}

	@Override
	public Optional<CompletionItem> visitConstructor(ConstructorMember member) {
		return Optional.empty();
	}

	@Override
	public Optional<CompletionItem> visitMethod(MethodMember member) {
		CompletionItem completionItem = new CompletionItem();
		completionItem.setKind(CompletionItemKind.Method);
		completionItem.setLabel(member.name + RenderingTypeVisitor.renderFunctionHeader(member.getHeader()));
		completionItem.setDocumentation(member.getPosition().toLongString());
		return Optional.of(completionItem);
	}

	@Override
	public Optional<CompletionItem> visitGetter(GetterMember member) {
		CompletionItem completionItem = new CompletionItem();
		completionItem.setKind(CompletionItemKind.Field);
		completionItem.setLabel(member.name);
		completionItem.setDocumentation(member.getPosition().toLongString());
		return Optional.of(completionItem);
	}

	@Override
	public Optional<CompletionItem> visitSetter(SetterMember member) {
		CompletionItem completionItem = new CompletionItem();
		completionItem.setKind(CompletionItemKind.Field);
		completionItem.setLabel(member.name);
		completionItem.setDocumentation(member.getPosition().toLongString());
		return Optional.of(completionItem);
	}

	@Override
	public Optional<CompletionItem> visitOperator(OperatorMember member) {
		return Optional.empty();
	}

	@Override
	public Optional<CompletionItem> visitCaster(CasterMember member) {
		return Optional.empty();
	}

	@Override
	public Optional<CompletionItem> visitCustomIterator(IteratorMember member) {
		return Optional.empty();
	}

	@Override
	public Optional<CompletionItem> visitImplementation(ImplementationMember member) {
		return Optional.empty();
	}

	@Override
	public Optional<CompletionItem> visitInnerDefinition(InnerDefinitionMember member) {
		return Optional.empty();
	}

	@Override
	public Optional<CompletionItem> visitStaticInitializer(StaticInitializerMember member) {
		return Optional.empty();
	}
}
