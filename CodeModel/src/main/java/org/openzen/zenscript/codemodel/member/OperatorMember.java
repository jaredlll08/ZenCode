package org.openzen.zenscript.codemodel.member;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.*;
import org.openzen.zenscript.codemodel.identifiers.instances.MethodInstance;
import org.openzen.zenscript.codemodel.type.TypeID;
import org.openzen.zenscript.codemodel.type.member.MemberSet;

import java.util.Optional;

public class OperatorMember extends FunctionalMember {
	public final OperatorType operator;
	private MethodInstance overrides;

	public OperatorMember(
			CodePosition position,
			HighLevelDefinition definition,
			Modifiers modifiers,
			OperatorType operator,
			FunctionHeader header) {
		super(position, definition, modifiers, header);

		this.operator = operator;
	}

	@Override
	public String getCanonicalName() {
		return definition.getFullName() + ":" + operator.operator + header.getCanonical();
	}

	@Override
	public FunctionalKind getKind() {
		return FunctionalKind.OPERATOR;
	}

	@Override
	public String describe() {
		return operator.operator + header.toString();
	}

	@Override
	public void registerTo(TypeID targetType, MemberSet.Builder members, GenericMapper mapper) {
		members.operator(mapper.map(targetType, this));
	}

	@Override
	public <T> T accept(MemberVisitor<T> visitor) {
		return visitor.visitOperator(this);
	}

	@Override
	public <C, R> R accept(C context, MemberVisitorWithContext<C, R> visitor) {
		return visitor.visitOperator(context, this);
	}

	@Override
	public Modifiers getEffectiveModifiers() {
		Modifiers result = super.getEffectiveModifiers();
		if (overrides != null) {
			if (overrides.getModifiers().isPublic())
				result = result.withPublic();
			if (overrides.getModifiers().isProtected())
				result = result.withProtected();
		}
		return result;
	}

	@Override
	public String getName() {
		return operator.operator;
	}

	@Override
	public Optional<OperatorType> getOperator() {
		return Optional.of(operator);
	}

	@Override
	public Optional<MethodInstance> getOverrides() {
		return Optional.ofNullable(overrides);
	}

	public void setOverrides(MethodInstance overrides) {
		this.overrides = overrides;
		header = header.inferFromOverride(overrides.getHeader());
	}
}
