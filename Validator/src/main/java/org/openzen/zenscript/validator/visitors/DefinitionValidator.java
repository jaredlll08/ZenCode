/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.validator.visitors;

import org.openzen.zenscript.codemodel.*;
import org.openzen.zenscript.codemodel.definition.*;
import org.openzen.zenscript.codemodel.member.EnumConstantMember;
import org.openzen.zenscript.codemodel.member.IDefinitionMember;
import org.openzen.zenscript.codemodel.type.TypeID;
import org.openzen.zenscript.validator.TypeContext;
import org.openzen.zenscript.validator.Validator;
import org.openzen.zenscript.validator.analysis.StatementScope;

import static org.openzen.zenscript.codemodel.Modifiers.*;

/**
 * @author Hoofdgebruiker
 */
public class DefinitionValidator implements DefinitionVisitor<Void> {
	private final Validator validator;

	public DefinitionValidator(Validator validator) {
		this.validator = validator;
	}

	@Override
	public Void visitClass(ClassDefinition definition) {
		ValidationUtils.validateModifiers(
				validator,
				definition.modifiers,
				FLAG_PUBLIC | FLAG_INTERNAL | FLAG_PRIVATE | FLAG_ABSTRACT | FLAG_STATIC | FLAG_PROTECTED | FLAG_VIRTUAL,
				definition.position,
				"Invalid class modifier");
		ValidationUtils.validateIdentifier(
				validator,
				definition.position,
				definition.name);

		if (definition.getSuperType() != null)
			definition.getSuperType().accept(new SupertypeValidator(validator, definition.position, definition));

		validateMembers(definition, DefinitionMemberContext.DEFINITION);
		return null;
	}

	@Override
	public Void visitInterface(InterfaceDefinition definition) {
		ValidationUtils.validateModifiers(
				validator,
				definition.modifiers,
				FLAG_PUBLIC | FLAG_INTERNAL | FLAG_PROTECTED | FLAG_PRIVATE,
				definition.position,
				"Invalid interface modifier");
		ValidationUtils.validateIdentifier(
				validator,
				definition.position,
				definition.name);

		validateMembers(definition, DefinitionMemberContext.DEFINITION);
		return null;
	}

	@Override
	public Void visitEnum(EnumDefinition definition) {
		ValidationUtils.validateModifiers(
				validator,
				definition.modifiers,
				FLAG_PUBLIC | FLAG_INTERNAL | FLAG_PROTECTED | FLAG_PRIVATE,
				definition.position,
				"Invalid enum modifier");
		ValidationUtils.validateIdentifier(
				validator,
				definition.position,
				definition.name);

		validateMembers(definition, DefinitionMemberContext.DEFINITION);
		return null;
	}

	@Override
	public Void visitStruct(StructDefinition definition) {
		int validModifiers = FLAG_PUBLIC | FLAG_INTERNAL | FLAG_PROTECTED | FLAG_PRIVATE;
		if (definition.outerDefinition != null)
			validModifiers |= FLAG_STATIC;

		ValidationUtils.validateModifiers(
				validator,
				definition.modifiers,
				validModifiers,
				definition.position,
				"Invalid struct modifier");
		ValidationUtils.validateIdentifier(
				validator,
				definition.position,
				definition.name);

		validateMembers(definition, DefinitionMemberContext.DEFINITION);
		return null;
	}

	@Override
	public Void visitFunction(FunctionDefinition definition) {
		ValidationUtils.validateModifiers(
				validator,
				definition.modifiers,
				FLAG_PUBLIC | FLAG_INTERNAL | FLAG_PROTECTED | FLAG_PRIVATE,
				definition.position,
				"Invalid function modifier");
		ValidationUtils.validateIdentifier(
				validator,
				definition.position,
				definition.name);

		StatementValidator statementValidator = new StatementValidator(validator, new FunctionStatementScope(definition.header));
		definition.caller.body.accept(statementValidator);
		return null;
	}

	@Override
	public Void visitExpansion(ExpansionDefinition definition) {
		ValidationUtils.validateModifiers(
				validator,
				definition.modifiers,
				FLAG_PUBLIC | FLAG_INTERNAL | FLAG_PROTECTED | FLAG_PRIVATE,
				definition.position,
				"Invalid expansion modifier");

		new TypeValidator(validator, definition.position).validate(TypeContext.EXPANSION_TARGET_TYPE, definition.target);
		validateMembers(definition, DefinitionMemberContext.EXPANSION);
		return null;
	}

	@Override
	public Void visitAlias(AliasDefinition definition) {
		ValidationUtils.validateModifiers(
				validator,
				definition.modifiers,
				FLAG_PUBLIC | FLAG_INTERNAL | FLAG_PROTECTED | FLAG_PRIVATE,
				definition.position,
				"Invalid alias modifier");
		ValidationUtils.validateIdentifier(
				validator,
				definition.position,
				definition.name);

		return null;
	}

	private void validateMembers(HighLevelDefinition definition, DefinitionMemberContext context) {
		DefinitionMemberValidator memberValidator = new DefinitionMemberValidator(validator, definition, context);
		for (IDefinitionMember member : definition.members) {
			member.accept(memberValidator);
		}
		if (definition instanceof EnumDefinition) {
			for (EnumConstantMember constant : ((EnumDefinition) definition).enumConstants) {
				memberValidator.visitEnumConstant(constant);
			}
		}
	}

	@Override
	public Void visitVariant(VariantDefinition variant) {
		ValidationUtils.validateModifiers(
				validator,
				variant.modifiers,
				FLAG_PUBLIC | FLAG_INTERNAL | FLAG_PROTECTED | FLAG_PRIVATE,
				variant.position,
				"Invalid variant modifier");
		ValidationUtils.validateIdentifier(
				validator,
				variant.position,
				variant.name);

		for (VariantDefinition.Option option : variant.options)
			validate(option);

		validateMembers(variant, DefinitionMemberContext.DEFINITION);
		return null;
	}

	private void validate(VariantDefinition.Option option) {
		ValidationUtils.validateIdentifier(validator, option.position, option.name);
		TypeValidator typeValidator = new TypeValidator(validator, option.position);
		for (TypeID type : option.types)
			typeValidator.validate(TypeContext.OPTION_MEMBER_TYPE, type);
	}

	private class FunctionStatementScope implements StatementScope {
		private final FunctionHeader header;

		public FunctionStatementScope(FunctionHeader header) {
			this.header = header;
		}

		@Override
		public boolean isConstructor() {
			return false;
		}

		@Override
		public boolean isStatic() {
			return true;
		}

		@Override
		public FunctionHeader getFunctionHeader() {
			return header;
		}

		@Override
		public boolean isStaticInitializer() {
			return false;
		}

		@Override
		public HighLevelDefinition getDefinition() {
			return null;
		}
	}
}
