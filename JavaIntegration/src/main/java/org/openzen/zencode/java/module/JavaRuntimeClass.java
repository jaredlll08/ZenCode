package org.openzen.zencode.java.module;

import org.objectweb.asm.Type;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.GenericMapper;
import org.openzen.zenscript.codemodel.Modifiers;
import org.openzen.zenscript.codemodel.Module;
import org.openzen.zenscript.codemodel.compilation.ResolvedType;
import org.openzen.zenscript.codemodel.generic.ParameterTypeBound;
import org.openzen.zenscript.codemodel.generic.TypeParameter;
import org.openzen.zenscript.codemodel.identifiers.TypeSymbol;
import org.openzen.zenscript.codemodel.type.DefinitionTypeID;
import org.openzen.zenscript.codemodel.type.TypeID;
import org.openzen.zenscript.javashared.JavaClass;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Optional;

public class JavaRuntimeClass implements TypeSymbol {
	public final JavaNativeModule module;
	public final JavaClass javaClass;
	public final Class<?> cls;

	private final Modifiers modifiers;
	private final JavaNativeTypeTemplate template;
	private final TypeParameter[] typeParameters;

	public JavaRuntimeClass(JavaNativeModule module, Class<?> cls, JavaClass.Kind kind) {
		this.module = module;
		this.javaClass = JavaClass.fromInternalName(Type.getInternalName(cls), kind);
		this.cls = cls;
		this.modifiers = translateModifiers(cls.getModifiers());
		this.template = new JavaNativeTypeTemplate(module, this);
		this.typeParameters = translateTypeParameters(cls);
	}

	@Override
	public Module getModule() {
		return module.getModule();
	}

	@Override
	public String describe() {
		return cls.getName();
	}

	@Override
	public boolean isInterface() {
		return cls.isInterface();
	}

	@Override
	public boolean isExpansion() {
		return javaClass.kind == JavaClass.Kind.EXPANSION;
	}

	@Override
	public Modifiers getModifiers() {
		return modifiers;
	}

	@Override
	public boolean isStatic() {
		return modifiers.isStatic();
	}

	@Override
	public boolean isEnum() {
		return cls.isEnum();
	}

	@Override
	public String getName() {
		return cls.getName();
	}

	@Override
	public ResolvedType resolve(TypeID[] typeArguments) {
		return new JavaNativeTypeMembers(template, DefinitionTypeID.create(this, typeArguments), GenericMapper.create(typeParameters, typeArguments));
	}

	@Override
	public TypeParameter[] getTypeParameters() {
		return typeParameters;
	}

	@Override
	public Optional<TypeSymbol> getOuter() {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> getSupertype(TypeID[] typeArguments) {
		return Optional.empty();
	}

	private static Modifiers translateModifiers(int modifiers) {
		Modifiers result = Modifiers.NONE;
		if ((modifiers & Modifier.FINAL) == 0)
			result = result.withVirtual();
		if ((modifiers & Modifier.PUBLIC) > 0)
			result = result.withPublic();
		else
			result = result.withPrivate();

		return result;
	}

	private static TypeParameter[] translateTypeParameters(Class<?> cls) {
		TypeVariable<?>[] javaTypeParameters = cls.getTypeParameters();
		TypeParameter[] typeParameters = TypeParameter.NONE;
		if (javaTypeParameters.length > 0) {
			typeParameters = new TypeParameter[cls.getTypeParameters().length];
		}

		for (int i = 0; i < javaTypeParameters.length; i++) {
			TypeVariable<?> typeVariable = javaTypeParameters[i];
			typeParameters[i] = new TypeParameter(CodePosition.NATIVE, typeVariable.getName());
		}

		for (int i = 0; i < javaTypeParameters.length; i++) {
			TypeVariable<?> typeVariable = javaTypeParameters[i];
			TypeParameter parameter = typeParameters[i];
			for (AnnotatedType bound : typeVariable.getAnnotatedBounds()) {
				if (bound.getType() == Object.class) {
					continue; //Makes the stdlib types work as they have "no" bounds for T
				}
				TypeID type = typeConverter.loadType(typeConversionContext.context, bound);
				parameter.addBound(new ParameterTypeBound(CodePosition.NATIVE, type));
			}
		}
	}
}
