package org.openzen.zenscript.codemodel.member;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zencode.shared.Taggable;
import org.openzen.zenscript.codemodel.HighLevelDefinition;
import org.openzen.zenscript.codemodel.Modifiers;
import org.openzen.zenscript.codemodel.annotations.MemberAnnotation;

public abstract class DefinitionMember extends Taggable implements IDefinitionMember {
	public final CodePosition position;
	public final HighLevelDefinition definition;
	protected final Modifiers modifiers;
	public MemberAnnotation[] annotations = MemberAnnotation.NONE;

	public DefinitionMember(CodePosition position, HighLevelDefinition definition, Modifiers modifiers) {
		this.position = position;
		this.definition = definition;
		this.modifiers = modifiers;
	}

	@Override
	public final CodePosition getPosition() {
		return position;
	}

	@Override
	public Modifiers getSpecifiedModifiers() {
		return modifiers;
	}

	@Override
	public final HighLevelDefinition getDefinition() {
		return definition;
	}

	@Override
	public MemberAnnotation[] getAnnotations() {
		return annotations;
	}

	@Override
	public String toString() {
		return describe();
	}

	public boolean isStatic() {
		return getEffectiveModifiers().isStatic();
	}

	public boolean isFinal() {
		return getEffectiveModifiers().isFinal();
	}

	public boolean isExtern() {
		return getEffectiveModifiers().isExtern();
	}

	public boolean isPrivate() {
		return getEffectiveModifiers().isPrivate();
	}

	public boolean isPublic() {
		return getEffectiveModifiers().isPublic();
	}

	public boolean isProtected() {
		return getEffectiveModifiers().isProtected();
	}

	public boolean isImplicit() {
		return getEffectiveModifiers().isImplicit();
	}
}
