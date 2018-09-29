/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.codemodel.scope;

import java.util.List;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zencode.shared.CompileException;
import org.openzen.zencode.shared.CompileExceptionCode;
import org.openzen.zenscript.codemodel.annotations.AnnotationDefinition;
import org.openzen.zenscript.codemodel.FunctionHeader;
import org.openzen.zenscript.codemodel.FunctionParameter;
import org.openzen.zenscript.codemodel.GenericMapper;
import org.openzen.zenscript.codemodel.expression.GetFunctionParameterExpression;
import org.openzen.zenscript.codemodel.expression.InvalidExpression;
import org.openzen.zenscript.codemodel.expression.LambdaClosure;
import org.openzen.zenscript.codemodel.partial.IPartialExpression;
import org.openzen.zenscript.codemodel.statement.LoopStatement;
import org.openzen.zenscript.codemodel.type.BasicTypeID;
import org.openzen.zenscript.codemodel.type.GenericName;
import org.openzen.zenscript.codemodel.type.StoredType;
import org.openzen.zenscript.codemodel.type.TypeID;
import org.openzen.zenscript.codemodel.type.member.LocalMemberCache;
import org.openzen.zenscript.codemodel.type.member.TypeMemberPreparer;
import org.openzen.zenscript.codemodel.type.storage.StorageTag;

/**
 *
 * @author Hoofdgebruiker
 */
public class LambdaScope extends StatementScope {
	private final BaseScope outer;
	private final FunctionHeader header;
	private final LambdaClosure closure;
	
	public LambdaScope(BaseScope outer, LambdaClosure closure, FunctionHeader header) {
		this.outer = outer;
		this.header = header;
		this.closure = closure;
	}
	
	@Override
	public LocalMemberCache getMemberCache() {
		return outer.getMemberCache();
	}
	
	@Override
	public IPartialExpression get(CodePosition position, GenericName name) throws CompileException {
		IPartialExpression outer = this.outer.get(position, name);
		if (outer == null) {
			if (name.hasNoArguments()) {
				for (FunctionParameter parameter : header.parameters) {
					if (parameter.name.equals(name.name)) {
						if (parameter.type.isBasic(BasicTypeID.UNDETERMINED))
							throw new CompileException(position, CompileExceptionCode.CALL_NO_VALID_METHOD, "parameter with undetermined type");
						
						return new GetFunctionParameterExpression(position, parameter);
					}
				}
			}

			return null;
		}
		
		return outer.capture(position, closure);
	}

	@Override
	public LoopStatement getLoop(String name) {
		return null;
	}

	@Override
	public FunctionHeader getFunctionHeader() {
		return header;
	}

	@Override
	public TypeID getType(CodePosition position, List<GenericName> name) {
		return outer.getType(position, name);
	}

	@Override
	public StorageTag getStorageTag(CodePosition position, String name, String[] parameters) {
		return outer.getStorageTag(position, name, parameters);
	}

	@Override
	public StoredType getThisType() {
		return outer.getThisType();
	}

	@Override
	public DollarEvaluator getDollar() {
		DollarEvaluator outerDollar = outer.getDollar();
		if (outerDollar == null)
			return null;
		
		return position -> outerDollar.apply(position).capture(position, closure).eval();
	}
	
	@Override
	public IPartialExpression getOuterInstance(CodePosition position) throws CompileException {
		return outer.getOuterInstance(position);
	}

	@Override
	public AnnotationDefinition getAnnotation(String name) {
		return outer.getAnnotation(name);
	}

	@Override
	public TypeMemberPreparer getPreparer() {
		return outer.getPreparer();
	}

	@Override
	public GenericMapper getLocalTypeParameters() {
		return outer.getLocalTypeParameters();
	}
}
