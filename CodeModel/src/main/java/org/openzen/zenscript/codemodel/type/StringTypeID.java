/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.codemodel.type;

import java.util.List;
import java.util.Set;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.GenericMapper;
import org.openzen.zenscript.codemodel.HighLevelDefinition;
import org.openzen.zenscript.codemodel.expression.ConstantStringExpression;
import org.openzen.zenscript.codemodel.expression.Expression;
import org.openzen.zenscript.codemodel.generic.TypeParameter;

/**
 *
 * @author Hoofdgebruiker
 */
public class StringTypeID implements TypeID {
	public static final StringTypeID INSTANCE = new StringTypeID();
	
	private StringTypeID() {}
	
	@Override
	public Expression getDefaultValue() {
		return new ConstantStringExpression(CodePosition.UNKNOWN, "");
	}
	
	@Override
	public TypeID getNormalized() {
		return INSTANCE;
	}
	
	@Override
	public <R> R accept(TypeVisitor<R> visitor) {
		return visitor.visitString(this);
	}

	@Override
	public <C, R, E extends Exception> R accept(C context, TypeVisitorWithContext<C, R, E> visitor) throws E {
		return visitor.visitString(context, this);
	}

	@Override
	public boolean hasDefaultValue() {
		return true;
	}
	
	@Override
	public boolean isDestructible() {
		return false;
	}
	
	@Override
	public boolean isDestructible(Set<HighLevelDefinition> scanning) {
		return false;
	}
	
	@Override
	public boolean isValueType() {
		return false;
	}

	@Override
	public TypeID instance(GenericMapper mapper) {
		return this;
	}

	@Override
	public boolean hasInferenceBlockingTypeParameters(TypeParameter[] parameters) {
		return false;
	}

	@Override
	public void extractTypeParameters(List<TypeParameter> typeParameters) {
		
	}

	@Override
	public TypeID getSuperType(GlobalTypeRegistry registry) {
		return null;
	}
	
	@Override
	public String toString() {
		return "string";
	}

	@Override
	public int hashCode() {
		return 1278;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}
}
