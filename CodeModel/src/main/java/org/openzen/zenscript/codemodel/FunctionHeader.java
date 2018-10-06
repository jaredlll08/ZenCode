/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.codemodel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openzen.zenscript.codemodel.expression.CallArguments;
import org.openzen.zenscript.codemodel.expression.Expression;
import org.openzen.zenscript.codemodel.generic.TypeParameter;
import org.openzen.zenscript.codemodel.type.BasicTypeID;
import org.openzen.zenscript.codemodel.type.GlobalTypeRegistry;
import org.openzen.zenscript.codemodel.scope.TypeScope;
import org.openzen.zenscript.codemodel.type.StoredType;
import org.openzen.zenscript.codemodel.type.member.LocalMemberCache;
import org.openzen.zenscript.codemodel.type.storage.StorageTag;

/**
 *
 * @author Hoofdgebruiker
 */
public class FunctionHeader {
	public final TypeParameter[] typeParameters;
	private StoredType returnType;
	public final FunctionParameter[] parameters;
	public final StoredType thrownType;
	public final StorageTag storage;
	
	public final int minParameters;
	public final int maxParameters;
	public final boolean hasUnknowns;
	
	public FunctionHeader(StoredType returnType) {
		if (returnType == null)
			throw new NullPointerException();
		
		this.typeParameters = TypeParameter.NONE;
		this.returnType = returnType;
		this.parameters = FunctionParameter.NONE;
		this.thrownType = null;
		this.storage = null;
		
		minParameters = 0;
		maxParameters = 0;
		hasUnknowns = returnType == BasicTypeID.UNDETERMINED.stored;
	}
	
	public FunctionHeader(BasicTypeID returnType) {
		this(returnType.stored);
	}
	
	public FunctionHeader(StoredType returnType, StoredType... parameterTypes) {
		if (returnType == null)
			throw new NullPointerException();
		
		this.typeParameters = TypeParameter.NONE;
		this.returnType = returnType;
		this.parameters = new FunctionParameter[parameterTypes.length];
		this.thrownType = null;
		this.storage = null;
		
		for (int i = 0; i < parameterTypes.length; i++)
			parameters[i] = new FunctionParameter(parameterTypes[i], null);
		
		minParameters = parameterTypes.length;
		maxParameters = parameterTypes.length;
		hasUnknowns = hasUnknowns(parameterTypes, returnType);
	}
	
	public FunctionHeader(BasicTypeID returnType, StoredType... parameterTypes) {
		this(returnType.stored, parameterTypes);
	}
	
	public FunctionHeader(StoredType returnType, FunctionParameter... parameters) {
		if (returnType == null)
			throw new NullPointerException();
		
		this.typeParameters = TypeParameter.NONE;
		this.returnType = returnType;
		this.parameters = parameters;
		this.thrownType = null;
		this.storage = null;
		
		minParameters = getMinParameters(parameters);
		maxParameters = getMaxParameters(parameters);
		hasUnknowns = hasUnknowns(parameters, returnType);
	}
	
	public FunctionHeader(BasicTypeID returnType, FunctionParameter... parameters) {
		this(returnType.stored, parameters);
	}
	
	public FunctionHeader(TypeParameter[] typeParameters, StoredType returnType, StoredType thrownType, StorageTag storage, FunctionParameter... parameters) {
		if (returnType == null)
			throw new NullPointerException();
		if (typeParameters == null)
			throw new NullPointerException();
		
		this.typeParameters = typeParameters;
		this.returnType = returnType;
		this.parameters = parameters;
		this.thrownType = thrownType;
		this.storage = storage;
		
		minParameters = getMinParameters(parameters);
		maxParameters = getMaxParameters(parameters);
		hasUnknowns = hasUnknowns(parameters, returnType);
	}
	
	public StoredType getReturnType() {
		return returnType;
	}
	
	public void setReturnType(StoredType returnType) {
		if (returnType == null)
			throw new NullPointerException();
		
		this.returnType = returnType;
	}
	
	public boolean isDenormalized() {
		if (returnType.getNormalized() != returnType)
			return true;
		for (FunctionParameter parameter : parameters)
			if (parameter.type.getNormalized() != parameter.type)
				return true;
		
		return false;
	}
	
	public FunctionHeader normalize(GlobalTypeRegistry registry) {
		if (!isDenormalized())
			return this;
		
		FunctionParameter[] normalizedParameters = new FunctionParameter[parameters.length];
		for (int i = 0; i < normalizedParameters.length; i++)
			normalizedParameters[i] = parameters[i].normalized(registry);
		return new FunctionHeader(typeParameters, returnType.getNormalized(), thrownType == null ? null : thrownType.getNormalized(), storage, normalizedParameters);
	}
	
	public int getNumberOfTypeParameters() {
		return typeParameters.length;
	}
	
	public boolean hasAnyDefaultValues() {
		for (FunctionParameter parameter : parameters)
			if (parameter.defaultValue != null)
				return true;
		
		return false;
	}
	
	public FunctionHeader withReturnType(StoredType returnType) {
		return new FunctionHeader(typeParameters, returnType, thrownType, storage, parameters);
	}
	
	public FunctionHeader inferFromOverride(GlobalTypeRegistry registry, FunctionHeader overridden) {
		TypeParameter[] resultTypeParameters = typeParameters;
		StoredType resultReturnType = this.returnType;
		if (resultReturnType.type == BasicTypeID.UNDETERMINED)
			resultReturnType = overridden.returnType;
		
		StoredType resultThrownType = this.thrownType;
		if (resultThrownType == null && overridden.thrownType != null)
			resultThrownType = overridden.thrownType;
		
		FunctionParameter[] resultParameters = Arrays.copyOf(parameters, parameters.length);
		for (int i = 0; i < resultParameters.length; i++) {
			if (resultParameters[i].type.type == BasicTypeID.UNDETERMINED) {
				FunctionParameter parameter = resultParameters[i];
				FunctionParameter original = overridden.parameters[i];
				resultParameters[i] = new FunctionParameter(original.type, parameter.name, parameter.defaultValue, original.variadic);
			}
		}
		
		StorageTag resultStorage = this.storage;
		if (resultStorage == null)
			resultStorage = overridden.storage;
		
		return new FunctionHeader(resultTypeParameters, resultReturnType, resultThrownType, resultStorage, resultParameters);
	}
	
	public boolean matchesExactly(CallArguments arguments, TypeScope scope) {
		if (arguments.arguments.length < minParameters || arguments.arguments.length > maxParameters)
			return false;
		
		FunctionHeader header = fillGenericArguments(scope.getTypeRegistry(), arguments.typeArguments, scope.getLocalTypeParameters());
		for (int i = 0; i < header.parameters.length; i++) {
			if (!arguments.arguments[i].type.equals(header.parameters[i].type))
				return false;
		}
		
		return true;
	}
	
	public boolean matchesImplicitly(CallArguments arguments, TypeScope scope) {
		if (arguments.arguments.length < minParameters || arguments.arguments.length > maxParameters)
			return false;
		
		FunctionHeader header = fillGenericArguments(scope.getTypeRegistry(), arguments.typeArguments, scope.getLocalTypeParameters());
		for (int i = 0; i < header.parameters.length; i++) {
			if (!scope.getTypeMembers(arguments.arguments[i].type).canCastImplicit(header.parameters[i].type))
				return false;
		}
		
		return true;
	}
	
	public String getCanonical() {
		StringBuilder result = new StringBuilder();
		if (getNumberOfTypeParameters() > 0) {
			result.append('<');
			for (int i = 0; i < typeParameters.length; i++) {
				if (i > 0)
					result.append(',');
				result.append(typeParameters[i].getCanonical());
			}
			result.append('>');
		}
		result.append('(');
		for (int i = 0; i < parameters.length; i++) {
			if (i > 0)
				result.append(',');
			result.append(parameters[i].type.toString());
		}
		result.append(')');
		return result.toString();
	}
	
	public StoredType[] inferTypes(LocalMemberCache cache, CallArguments arguments, List<StoredType> resultHint) {
		if (arguments.arguments.length != this.parameters.length)
			return null;
		
		Map<TypeParameter, StoredType> mapping = new HashMap<>();
		if (!resultHint.isEmpty()) {
			for (StoredType hint : resultHint) {
				Map<TypeParameter, StoredType> temp = returnType.inferTypeParameters(cache, hint);
				if (temp != null) {
					mapping = temp;
					break;
				}
			}
		}
		
		// TODO: lambda header inference
		for (int i = 0; i < parameters.length; i++) {
			Map<TypeParameter, StoredType> forParameter = parameters[i].type.inferTypeParameters(cache, arguments.arguments[i].type);
			if (forParameter == null)
				return null;
			
			mapping.putAll(forParameter);
		}
		
		if (mapping.size() > typeParameters.length)
			return null;
		
		StoredType[] result = new StoredType[typeParameters.length];
		for (int i = 0; i < typeParameters.length; i++) {
			TypeParameter typeParameter = typeParameters[i];
			if (!mapping.containsKey(typeParameter)) {
				return null;
			} else {
				result[i] = mapping.get(typeParameter);
			}
		}
		
		return result;
	}
	
	public boolean hasInferenceBlockingTypeParameters(TypeParameter[] parameters) {
		for (int i = 0; i < this.parameters.length; i++)
			if (this.parameters[i].type.hasInferenceBlockingTypeParameters(parameters))
				return true;
		
		return false;
	}
	
	public boolean accepts(TypeScope scope, Expression... arguments) {
		if (parameters.length != arguments.length)
			return false;
		
		for (int i = 0; i < arguments.length; i++) {
			if (!scope.getTypeMembers(arguments[i].type).canCastImplicit(parameters[i].type))
				return false;
		}
		
		return true;
	}
	
	public boolean canOverride(TypeScope scope, FunctionHeader other) {
		if (other == null)
			throw new NullPointerException();
		if (parameters.length != other.parameters.length)
			return false;
		if (returnType.type != BasicTypeID.UNDETERMINED && !scope.getTypeMembers(returnType).canCastImplicit(other.returnType))
			return false;
		
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].type.type == BasicTypeID.UNDETERMINED)
				continue;
			
			if (parameters[i].variadic != other.parameters[i].variadic)
				return false;
			if (!scope.getTypeMembers(other.parameters[i].type).canCastImplicit(parameters[i].type))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if two function headers are equivalent. Functions headers are
	 * equivalent if their types are the same.
	 * 
	 * @param other
	 * @return 
	 */
	public boolean isEquivalentTo(FunctionHeader other) {
		if (parameters.length != other.parameters.length)
			return false;
		
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].type != other.parameters[i].type)
				return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if two function headers are similar. "similar" means that there
	 * exists a set of parameters for which there is no way to determine which
	 * one to call.
	 * 
	 * Note that this does not mean that there is never confusion about which
	 * method to call. There can be confusion due to implicit conversions. This
	 * can be resolved by performing the conversions explicitly.
	 * 
	 * It is illegal to have two similar methods with the same name.
	 * 
	 * @param other
	 * @return 
	 */
	public boolean isSimilarTo(FunctionHeader other) {
		int common = Math.min(parameters.length, other.parameters.length);
		for (int i = 0; i < common; i++) {
			if (parameters[i].type != other.parameters[i].type)
				return false;
		}
		for (int i = common; i < parameters.length; i++) {
			if (parameters[i].defaultValue == null)
				return false;
		}
		for (int i = common; i < other.parameters.length; i++) {
			if (other.parameters[i].defaultValue == null)
				return false;
		}
		
		return true;
	}
	
	public FunctionHeader instanceForCall(GlobalTypeRegistry registry, CallArguments arguments) {
		if (arguments.getNumberOfTypeArguments() > 0) {
			Map<TypeParameter, StoredType> typeParameters = StoredType.getMapping(this.typeParameters, arguments.typeArguments);
			return instance(new GenericMapper(registry, typeParameters));
		} else {
			return this;
		}
	}
	
	public FunctionHeader withGenericArguments(GlobalTypeRegistry registry, GenericMapper mapper) {
		if (typeParameters.length > 0)
			mapper = mapper.getInner(registry, StoredType.getSelfMapping(registry, typeParameters));
		
		return instance(mapper);
	}
	
	private FunctionHeader instance(GenericMapper mapper) {
		StoredType returnType = this.returnType.instance(mapper);
		FunctionParameter[] parameters = new FunctionParameter[this.parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = this.parameters[i].withGenericArguments(mapper);
		}
		return new FunctionHeader(typeParameters, returnType, thrownType == null ? null : thrownType.instance(mapper), storage, parameters);
	}
	
	public FunctionHeader fillGenericArguments(GlobalTypeRegistry registry, StoredType[] arguments, GenericMapper typeParameterMapping) {
		if (arguments == null || arguments.length == 0)
			return this;
		
		Map<TypeParameter, StoredType> typeArguments = StoredType.getMapping(typeParameters, arguments);
		GenericMapper mapper = typeParameterMapping.getInner(registry, typeArguments);
		
		StoredType returnType = this.returnType.instance(mapper);
		FunctionParameter[] parameters = new FunctionParameter[this.parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = this.parameters[i].withGenericArguments(mapper);
		}
		return new FunctionHeader(TypeParameter.NONE, returnType, thrownType == null ? null : thrownType.instance(mapper), storage, parameters);
	}
	
	public FunctionHeader forTypeParameterInference() {
		return new FunctionHeader(BasicTypeID.UNDETERMINED.stored, parameters);
	}
	
	public FunctionHeader forLambda(FunctionHeader lambdaHeader) {
		FunctionParameter[] parameters = new FunctionParameter[lambdaHeader.parameters.length];
		for (int i = 0; i < lambdaHeader.parameters.length; i++)
			parameters[i] = new FunctionParameter(this.parameters[i].type, lambdaHeader.parameters[i].name);
		
		return new FunctionHeader(typeParameters, returnType, thrownType, storage, parameters);
	}
	
	public FunctionParameter getVariadicParameter() {
		if (parameters.length == 0)
			return null;
		if (parameters[parameters.length - 1].variadic)
			return parameters[parameters.length - 1];
		
		return null;
	}
	
	public String explainWhyIncompatible(TypeScope scope, CallArguments arguments) {
		if (this.parameters.length != arguments.arguments.length)
			return parameters.length + " parameters expected but " + arguments.arguments.length + " given.";
		
		if (getNumberOfTypeParameters() != arguments.getNumberOfTypeArguments())
			return getNumberOfTypeParameters() + " type parameters expected but " + arguments.getNumberOfTypeArguments() + " given.";
		
		for (int i = 0; i < parameters.length; i++) {
			if (!scope.getTypeMembers(arguments.arguments[i].type).canCastImplicit(parameters[i].type)) {
				return "Parameter " + i + ": cannot cast " + arguments.arguments[i].type + " to " + parameters[i].type;
			}
		}
		
		return "Method should be compatible";
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (typeParameters.length > 0) {
			result.append("<");
			for (int i = 0; i < typeParameters.length; i++) {
				if (i > 0)
					result.append(", ");
				result.append(typeParameters[i].toString());
			}
			result.append(">");
		}
		result.append("(");
		for (int i = 0; i < parameters.length; i++) {
			if (i > 0)
				result.append(", ");
			result.append(parameters[i].toString());
		}
		result.append(") as ");
		result.append(returnType.toString());
		return result.toString();
	}

	private static int getMinParameters(FunctionParameter[] parameters) {
		for (int i = 0; i < parameters.length; i++)
			if (parameters[i].defaultValue != null || parameters[i].variadic)
				return i;
		
		return parameters.length;
	}
	
	private static int getMaxParameters(FunctionParameter[] parameters) {
		if (parameters.length == 0)
			return 0;
		
		return parameters[parameters.length - 1].variadic ? Integer.MAX_VALUE : parameters.length;
	}
	
	private static boolean hasUnknowns(StoredType[] types, StoredType returnType) {
		if (returnType.type == BasicTypeID.UNDETERMINED)
			return true;
		
		for (StoredType type : types)
			if (type.type == BasicTypeID.UNDETERMINED)
				return true;
		
		return false;
	}
	
	private static boolean hasUnknowns(FunctionParameter[] parameters, StoredType returnType) {
		if (returnType.type == BasicTypeID.UNDETERMINED)
			return true;
		
		for (FunctionParameter parameter : parameters)
			if (parameter.type.type == BasicTypeID.UNDETERMINED)
				return true;
		
		return false;
	}

	public boolean accepts(int arguments) {
		return arguments >= this.minParameters && arguments <= this.maxParameters;
	}
}
