/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.javashared;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zencode.shared.logging.IZSLogger;
import org.openzen.zenscript.codemodel.FunctionHeader;
import org.openzen.zenscript.codemodel.FunctionParameter;
import org.openzen.zenscript.codemodel.HighLevelDefinition;
import org.openzen.zenscript.codemodel.identifiers.ModuleSymbol;
import org.openzen.zenscript.codemodel.definition.VariantDefinition;
import org.openzen.zenscript.codemodel.definition.ZSPackage;
import org.openzen.zenscript.codemodel.generic.TypeParameter;
import org.openzen.zenscript.codemodel.identifiers.DefinitionSymbol;
import org.openzen.zenscript.codemodel.identifiers.FieldSymbol;
import org.openzen.zenscript.codemodel.identifiers.MethodSymbol;
import org.openzen.zenscript.codemodel.identifiers.TypeSymbol;
import org.openzen.zenscript.codemodel.identifiers.instances.FieldInstance;
import org.openzen.zenscript.codemodel.identifiers.instances.MethodInstance;
import org.openzen.zenscript.codemodel.member.ImplementationMember;
import org.openzen.zenscript.codemodel.member.ref.VariantOptionInstance;
import org.openzen.zenscript.codemodel.type.*;
import org.openzen.zenscript.codemodel.type.builtin.RangeTypeSymbol;
import org.openzen.zenscript.javashared.compiling.JavaCompilingClass;
import org.openzen.zenscript.javashared.compiling.JavaCompilingModule;
import org.openzen.zenscript.javashared.types.JavaFunctionalInterfaceTypeID;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.*;

/**
 * @author Hoofdgebruiker
 */
public abstract class JavaContext {
	public final ZSPackage modulePackage;
	public final String basePackage;
	public final IZSLogger logger;
	private final Map<String, JavaSynthesizedFunction> functions = new HashMap<>();
	private final Map<String, JavaSynthesizedRange> ranges = new HashMap<>();
	private final JavaCompileSpace space;
	private final Map<ModuleSymbol, JavaCompiledModule> modules = new HashMap<>();
	private final JavaCompilingModule generatedClassesModule;

	public JavaContext(JavaCompileSpace space, ZSPackage modulePackage, String basePackage, IZSLogger logger) {
		this.logger = logger;
		this.space = space;

		this.modulePackage = modulePackage;
		this.basePackage = basePackage;

		ModuleSymbol generatedClassesModule = new ModuleSymbol("generated");
		JavaCompiledModule compiledGeneratedClassesModule = new JavaCompiledModule(generatedClassesModule, FunctionParameter.NONE);
		this.generatedClassesModule = new JavaCompilingModule(this, compiledGeneratedClassesModule);

		addDefaultFunctions();

		modules.put(ModuleSymbol.BUILTIN, JavaBuiltinModule.generate());
	}

	private void addDefaultFunctions() {

		final TypeParameter t = new TypeParameter(CodePosition.BUILTIN, "T");
		final GenericTypeID tType = new GenericTypeID(t);
		final TypeParameter u = new TypeParameter(CodePosition.BUILTIN, "U");
		final GenericTypeID uType = new GenericTypeID(u);
		final TypeParameter v = new TypeParameter(CodePosition.BUILTIN, "V");
		final GenericTypeID vType = new GenericTypeID(v);
		final Map<String, TypeParameter> typeParamMap = new HashMap<>();
		typeParamMap.put("T", t);
		typeParamMap.put("U", u);
		typeParamMap.put("V", v);

		final Function<String, TypeParameter> paramConverter = key -> typeParamMap.computeIfAbsent(key, newKey -> new TypeParameter(CodePosition.BUILTIN, newKey));
		registerFunction(paramConverter, BiConsumer.class);
		registerFunction(paramConverter, BiFunction.class);
		registerFunction(paramConverter, BiPredicate.class);
		registerFunction(paramConverter, BooleanSupplier.class);
		registerFunction(paramConverter, Consumer.class);
		registerFunction(paramConverter, DoubleBinaryOperator.class);
		registerFunction(paramConverter, DoubleConsumer.class);
		registerFunction(paramConverter, DoubleFunction.class);
		registerFunction(paramConverter, DoublePredicate.class);
		registerFunction(paramConverter, DoubleSupplier.class);
		registerFunction(paramConverter, DoubleToIntFunction.class);
		registerFunction(paramConverter, DoubleToLongFunction.class);
		registerFunction(paramConverter, DoubleUnaryOperator.class);
		registerFunction(paramConverter, Function.class);
		registerFunction(paramConverter, IntBinaryOperator.class);
		registerFunction(paramConverter, IntConsumer.class);
		registerFunction(paramConverter, IntFunction.class);
		registerFunction(paramConverter, IntPredicate.class);
		registerFunction(paramConverter, IntSupplier.class);
		registerFunction(paramConverter, IntToDoubleFunction.class);
		registerFunction(paramConverter, IntToLongFunction.class);
		registerFunction(paramConverter, IntUnaryOperator.class);
		registerFunction(paramConverter, LongBinaryOperator.class);
		registerFunction(paramConverter, LongConsumer.class);
		registerFunction(paramConverter, LongFunction.class);
		registerFunction(paramConverter, LongPredicate.class);
		registerFunction(paramConverter, LongSupplier.class);
		registerFunction(paramConverter, LongToDoubleFunction.class);
		registerFunction(paramConverter, LongToIntFunction.class);
		registerFunction(paramConverter, LongUnaryOperator.class);
		registerFunction(paramConverter, ObjDoubleConsumer.class);
		registerFunction(paramConverter, ObjIntConsumer.class);
		registerFunction(paramConverter, ObjLongConsumer.class);
		registerFunction(paramConverter, Predicate.class);
		registerFunction(paramConverter, Supplier.class);
		registerFunction(paramConverter, ToDoubleBiFunction.class);
		registerFunction(paramConverter, ToDoubleFunction.class);
		registerFunction(paramConverter, ToIntBiFunction.class);
		registerFunction(paramConverter, ToIntFunction.class);
		registerFunction(paramConverter, ToLongBiFunction.class);
		registerFunction(paramConverter, ToLongFunction.class);
		// Needs special handling due to it just implementing BiFunction/Function.
		registerFunction("TTToT", BinaryOperator.class, "apply", new TypeParameter[]{t}, tType, tType, tType);
		registerFunction("TToT", UnaryOperator.class, "apply", new TypeParameter[]{t}, tType);
		registerFunction("TTToInt", Comparator.class, "compare", new TypeParameter[]{t}, BasicTypeID.INT, new GenericTypeID(t), tType);
	}

	private void registerFunction(String id, Class<?> clazz, String methodName, TypeParameter[] typeParameters, TypeID returnType, TypeID... parameterTypes) {

		functions.put(id, new JavaSynthesizedFunction(
				new JavaClass(clazz.getPackage().getName(), clazz.getSimpleName(), JavaClass.Kind.INTERFACE),
				typeParameters,
				new FunctionHeader(returnType, parameterTypes),
				methodName));
	}

	private void registerFunction(Function<String, TypeParameter> paramConverter, Class<?> clazz) {
		final TypeParameter[] parameters = new TypeParameter[clazz.getTypeParameters().length];
		final Map<String, GenericTypeID> parameterMapping = new HashMap<>();
		for (int i = 0; i < clazz.getTypeParameters().length; i++) {
			final TypeParameter typeParameter = paramConverter.apply(getTypeParameter(i));
			parameters[i] = typeParameter;
			parameterMapping.put(clazz.getTypeParameters()[i].getName(), new GenericTypeID(typeParameter));
		}
		final Optional<Method> foundMethod = Arrays.stream(clazz.getMethods()).filter(method -> !Modifier.isStatic(method.getModifiers()) && !method.isDefault()).findFirst();
		if (foundMethod.isPresent()) {
			final Method method = foundMethod.get();
			final StringBuilder idBuilder = new StringBuilder();
			final TypeID[] parameterTypes = new TypeID[method.getParameterCount()];
			for (int i = 0; i < parameterTypes.length; i++) {
				final Type type = method.getGenericParameterTypes()[i];
				parameterTypes[i] = convertTypeToTypeID(parameterMapping, type);
				idBuilder.append(getNameFromType(parameterMapping, type));
			}
			final Type genericReturnType = method.getGenericReturnType();

			idBuilder.append("To").append(getNameFromType(parameterMapping, genericReturnType));

			if (functions.containsKey(idBuilder.toString())) {
				throw new IllegalArgumentException(String.format("Function '%s' already registered!", idBuilder));
			}

			functions.put(idBuilder.toString(), new JavaSynthesizedFunction(
					new JavaClass(clazz.getPackage().getName(), clazz.getSimpleName(), JavaClass.Kind.INTERFACE),
					parameters,
					new FunctionHeader(convertTypeToTypeID(parameterMapping, genericReturnType), parameterTypes),
					method.getName()));
		} else {
			throw new IllegalArgumentException(String.format("Unable to find any applicable methods in class: '%s'", clazz.getName()));
		}

	}

	private String getNameFromType(Map<String, GenericTypeID> paramMap, Type type) {
		if (paramMap.containsKey(type.getTypeName())) {
			return paramMap.get(type.getTypeName()).parameter.name;
		}
		// Could potentially replace this with JavaTypeNameVisitor.visitBasic()
		switch (type.getTypeName()) {
			case "int":
				return "Int";
			case "void":
				return "Void";
			case "boolean":
				return "Bool";
			case "byte":
				return "Byte";
			case "short":
				return "Short";
			case "long":
				return "Long";
			case "float":
				return "Float";
			case "double":
				return "Double";
			case "char":
				return "Char";
			default:
				throw new IllegalArgumentException(String.format("Unknown Type: '%s'", type));
		}
	}

	private TypeID convertTypeToTypeID(Map<String, GenericTypeID> paramMap, Type type) {
		if (paramMap.containsKey(type.getTypeName())) {
			return paramMap.get(type.getTypeName());
		}

		switch (type.getTypeName()) {
			case "int":
				return BasicTypeID.INT;
			case "void":
				return BasicTypeID.VOID;
			case "boolean":
				return BasicTypeID.BOOL;
			case "byte":
				return BasicTypeID.BYTE;
			case "short":
				return BasicTypeID.SHORT;
			case "long":
				return BasicTypeID.LONG;
			case "float":
				return BasicTypeID.FLOAT;
			case "double":
				return BasicTypeID.DOUBLE;
			case "char":
				return BasicTypeID.CHAR;
		}

		throw new IllegalArgumentException(String.format("Unknown Type: '%s'", type));
	}


	public String getPackageName(ZSPackage pkg) {
		if (pkg == null)
			throw new IllegalArgumentException("Package not part of this module");

		if (pkg == modulePackage)
			return basePackage;

		return getPackageName(pkg.parent) + "/" + pkg.name;
	}

	public JavaNativeMethod getFunctionalInterface(TypeID type) {
		if (type instanceof JavaFunctionalInterfaceTypeID) {
			JavaFunctionalInterfaceTypeID t = (JavaFunctionalInterfaceTypeID) type;
			return t.method;
		} else {
			FunctionTypeID functionType = (FunctionTypeID) type;
			JavaSynthesizedFunctionInstance function = getFunction(functionType);

			return new JavaNativeMethod(
					function.getCls(),
					JavaNativeMethod.Kind.INTERFACE,
					function.getMethod(),
					false,
					getMethodDescriptor(function.getHeader()),
					JavaModifiers.PUBLIC | JavaModifiers.ABSTRACT,
					function.getHeader().getReturnType().isGeneric());
		}
	}

	protected abstract JavaSyntheticClassGenerator getTypeGenerator();

	public abstract String getDescriptor(TypeID type);

	public String getSignature(TypeID type) {
		return new JavaTypeGenericVisitor(this).getGenericSignature(type);
	}

	public void addModule(ModuleSymbol module, JavaCompiledModule target) {
		modules.put(module, target);

		//TODO: can we do this here?
		space.register(target);
	}

	public JavaCompiledModule getJavaModule(ModuleSymbol module) {
		if (modules.containsKey(module))
			return modules.get(module);

		JavaCompiledModule javaModule = space.getCompiled(module);
		if (javaModule == null)
			throw new IllegalStateException("Module not yet registered: " + module.name);

		return javaModule;
	}

	public JavaClass getJavaClass(DefinitionSymbol definition) {
		return getJavaModule(definition.getModule()).getClassInfo(definition);
	}

	public JavaClass getJavaExpansionClass(DefinitionSymbol definition) {
		return getJavaModule(definition.getModule()).getExpansionClassInfo(definition);
	}

	public JavaClass optJavaClass(HighLevelDefinition definition) {
		return getJavaModule(definition.module).optClassInfo(definition);
	}

	public JavaNativeClass getJavaNativeClass(HighLevelDefinition definition) {
		return getJavaModule(definition.module).getNativeClassInfo(definition);
	}

	public boolean hasJavaClass(HighLevelDefinition definition) {
		return getJavaModule(definition.module).hasClassInfo(definition);
	}

	public void setJavaClass(HighLevelDefinition definition, JavaClass cls) {
		getJavaModule(definition.module).setClassInfo(definition, cls);
	}

	public void setJavaExpansionClass(HighLevelDefinition definition, JavaClass cls) {
		getJavaModule(definition.module).setExpansionClassInfo(definition, cls);
	}

	public void setJavaNativeClass(HighLevelDefinition definition, JavaNativeClass cls) {
		getJavaModule(definition.module).setNativeClassInfo(definition, cls);
	}

	public boolean hasJavaField(MethodSymbol getterOrSetter) {
		DefinitionSymbol definition = getterOrSetter.getDefiningType();
		return getJavaModule(definition.getModule()).optFieldInfo(getterOrSetter) != null;
	}

	public JavaField getJavaField(FieldSymbol field) {
		DefinitionSymbol definition = field.getDefiningType();
		return getJavaModule(definition.getModule()).getFieldInfo(field);
	}

	public JavaField getJavaField(FieldInstance member) {
		return getJavaField(member.field);
	}

	public JavaMethod getJavaMethod(MethodSymbol method) {
		return getJavaModule(method.getDefiningType().getModule()).getMethodInfo(method);
	}

	public JavaMethod getJavaMethod(MethodInstance member) {
		return getJavaMethod(member.method);
	}

	public JavaVariantOption getJavaVariantOption(VariantDefinition.Option option) {
		HighLevelDefinition definition = option.variant;
		return getJavaModule(definition.module).getVariantOption(option);
	}

	public JavaVariantOption getJavaVariantOption(VariantOptionInstance member) {
		return getJavaVariantOption(member.getOption());
	}

	public JavaImplementation getJavaImplementation(ImplementationMember member) {
		return getJavaModule(member.definition.module).getImplementationInfo(member);
	}

	public String getMethodDescriptor(FunctionHeader header) {
		return getMethodDescriptor(header, false, "");
	}

	public String getMethodDescriptorExpansion(FunctionHeader header, TypeID expandedType) {
		StringBuilder startBuilder = new StringBuilder(getDescriptor(expandedType));
		final List<TypeParameter> typeParameters = new ArrayList<>();
		expandedType.extractTypeParameters(typeParameters);
		for (TypeParameter typeParameter : typeParameters) {
			startBuilder.append("Ljava/lang/Class;");
		}

		return getMethodDescriptor(header, false, startBuilder.toString());
	}

	public String getMethodSignatureExpansion(FunctionHeader header, TypeID expandedClass) {
		return new JavaTypeGenericVisitor(this).getMethodSignatureExpansion(header, expandedClass);
	}

	public String getMethodSignature(FunctionHeader header) {
		return getMethodSignature(header, true);
	}

	public String getMethodSignature(FunctionHeader header, boolean withGenerics) {
		return getMethodSignature(header, withGenerics, false);
	}

	public String getMethodSignature(FunctionHeader header, boolean withGenerics, boolean voidReturnType) {
		return new JavaTypeGenericVisitor(this).getGenericMethodSignature(header, withGenerics, voidReturnType);
	}

	public String getEnumConstructorDescriptor(FunctionHeader header) {
		return getMethodDescriptor(header, true, "");
	}

	public JavaSynthesizedFunctionInstance getFunction(FunctionTypeID type) {
		String id = getFunctionId(type.header);
		JavaSynthesizedFunction function;
		if (!functions.containsKey(id)) {
			JavaClass cls = new JavaClass("zsynthetic", "Function" + id, JavaClass.Kind.INTERFACE);
			List<TypeParameter> typeParameters = new ArrayList<>();
			List<FunctionParameter> parameters = new ArrayList<>();

			// Ensures that parameters filled with the same generic arg are considered one generic parameter
			// function(string, string) -> function(T,T) instead of function(T, U)
			Map<TypeID, TypeParameter> alreadyKnownTypeParameters = new HashMap<>();

			for (FunctionParameter parameter : type.header.parameters) {
				JavaTypeInfo typeInfo = JavaTypeInfo.get(parameter.type);
				if (typeInfo.primitive) {
					parameters.add(new FunctionParameter(parameter.type, Character.toString((char) ('a' + parameters.size()))));
				} else {
					final TypeParameter typeParameter;
					if(alreadyKnownTypeParameters.containsKey(parameter.type)) {
						typeParameter = alreadyKnownTypeParameters.get(parameter.type);
					} else {
						typeParameter = new TypeParameter(CodePosition.BUILTIN, getTypeParameter(typeParameters.size()));
						typeParameters.add(typeParameter);
						alreadyKnownTypeParameters.put(parameter.type, typeParameter);
					}

					parameters.add(new FunctionParameter(new GenericTypeID(typeParameter), Character.toString((char) ('a' + parameters.size()))));
				}
			}
			TypeID returnType;
			{
				JavaTypeInfo typeInfo = JavaTypeInfo.get(type.header.getReturnType());
				if (typeInfo.primitive) {
					returnType = type.header.getReturnType();
				} else {
					TypeParameter typeParameter = new TypeParameter(CodePosition.BUILTIN, getTypeParameter(typeParameters.size()));
					typeParameters.add(typeParameter);
					returnType = new GenericTypeID(typeParameter);
				}
			}
			function = new JavaSynthesizedFunction(
					cls,
					typeParameters.toArray(new TypeParameter[typeParameters.size()]),
					new FunctionHeader(returnType, parameters.toArray(new FunctionParameter[parameters.size()])),
					"invoke");

			functions.put(id, function);
			getTypeGenerator().synthesizeFunction(function);
		} else {
			function = functions.get(id);
		}

		List<TypeID> typeArguments = new ArrayList<>();
		for (FunctionParameter parameter : type.header.parameters) {
			JavaTypeInfo typeInfo = JavaTypeInfo.get(parameter.type);
			if (!typeInfo.primitive) {
				typeArguments.add(parameter.type);
			}
		}
		if (!JavaTypeInfo.isPrimitive(type.header.getReturnType()))
			typeArguments.add(type.header.getReturnType());

		return new JavaSynthesizedFunctionInstance(function, typeArguments.toArray(new TypeID[typeArguments.size()]));
	}

	private String getFunctionId(FunctionHeader header) {
		StringBuilder signature = new StringBuilder();
		int typeParameterIndex = 0;

		//Check if we already know the type, so that function(String,String) becomes function(TT) instead of TU
		final Map<TypeID, String> alreadyKnownParameters = new HashMap<>();

		for (FunctionParameter parameter : header.parameters) {
			final String id;
			final TypeID parameterType = parameter.type;

			if(alreadyKnownParameters.containsKey(parameterType)) {
				id = alreadyKnownParameters.get(parameterType);
			} else {
				JavaTypeInfo typeInfo = JavaTypeInfo.get(parameterType);
				id = typeInfo.primitive ? parameterType.accept(new JavaSyntheticTypeSignatureConverter()) : getTypeParameter(typeParameterIndex++);
				alreadyKnownParameters.put(parameterType, id);
			}
			signature.append(id);
		}
		signature.append("To");
		{
			JavaTypeInfo typeInfo = JavaTypeInfo.get(header.getReturnType());
			String id = typeInfo.primitive ? header.getReturnType().accept(new JavaSyntheticTypeSignatureConverter()) : getTypeParameter(typeParameterIndex++);
			signature.append(id);
		}
		return signature.toString();
	}

	private String getTypeParameter(int index) {
		switch (index) {
			case 0:
				return "T";
			case 1:
				return "U";
			case 2:
				return "V";
			case 3:
				return "W";
			case 4:
				return "X";
			case 5:
				return "Y";
			case 6:
				return "Z";
			default:
				return "T" + index;
		}
	}

	public JavaSynthesizedClass getRange(RangeTypeID type) {
		JavaTypeInfo typeInfo = JavaTypeInfo.get(type.baseType);
		String id = typeInfo.primitive ? type.accept(new JavaSyntheticTypeSignatureConverter()) : "T";
		JavaSynthesizedRange range;
		if (!ranges.containsKey(id)) {
			JavaClass cls = new JavaClass("zsynthetic", id, JavaClass.Kind.CLASS);
			JavaCompilingClass class_ = new JavaCompilingClass(generatedClassesModule, RangeTypeSymbol.INSTANCE, cls, null);
			if (typeInfo.primitive) {
				range = new JavaSynthesizedRange(class_, TypeParameter.NONE, type.baseType);
			} else {
				TypeParameter typeParameter = new TypeParameter(CodePosition.BUILTIN, "T");
				range = new JavaSynthesizedRange(class_, new TypeParameter[]{typeParameter}, new GenericTypeID(typeParameter));
			}
			ranges.put(id, range);
			getTypeGenerator().synthesizeRange(range);
		} else {
			range = ranges.get(id);
		}

		if (typeInfo.primitive) {
			return new JavaSynthesizedClass(range.cls, TypeID.NONE);
		} else {
			return new JavaSynthesizedClass(range.cls, new TypeID[]{type.baseType});
		}
	}

	/**
	 * @param header            Function Header
	 * @param isEnumConstructor If this is an enum constructor, add String, int as parameters
	 * @param expandedType      If this is for an expanded type, add the type at the beginning.
	 *                          Can be null or an empty string if this is not an expansion method header
	 * @return Method descriptor {@code (<LClass;*No.TypeParameters><LString;I if enum><expandedType><headerTypes>)<retType> }
	 */
	public String getMethodDescriptor(FunctionHeader header, boolean isEnumConstructor, String expandedType) {
		StringBuilder descBuilder = new StringBuilder("(");
		for (int i = 0; i < header.getNumberOfTypeParameters(); i++)
			descBuilder.append("Ljava/lang/Class;");

		if (isEnumConstructor)
			descBuilder.append("Ljava/lang/String;I");

		//TODO: Put this earlier? We'd need to agree on one...
		if (expandedType != null)
			descBuilder.append(expandedType);

		for (FunctionParameter parameter : header.parameters) {
			descBuilder.append(getDescriptor(parameter.type));
		}
		descBuilder.append(")");
		descBuilder.append(getDescriptor(header.getReturnType()));
		return descBuilder.toString();
	}

	public String getMethodDescriptorConstructor(MethodSymbol method) {
		// In Java, the .ctor does not return the type, so let's override the return type
		final FunctionHeader javaHeader = new FunctionHeader(
				method.getHeader().typeParameters,
				BasicTypeID.VOID,
				method.getHeader().thrownType,
				method.getHeader().parameters);

		StringBuilder startBuilder = new StringBuilder();
		DefinitionSymbol type = method.getDefiningType();
		for (TypeParameter typeParameter : type.getTypeParameters()) {
			startBuilder.append("Ljava/lang/Class;");
		}
		return getMethodDescriptor(javaHeader, type.asType().map(TypeSymbol::isEnum).orElse(false), startBuilder.toString());
	}

	public String getMethodSignatureConstructor(MethodSymbol method) {
		FunctionParameter[] parameters;

		if(method.getDefiningType().isEnum()) {
			final int parameterCount = method.getHeader().parameters.length;
			parameters = new FunctionParameter[parameterCount + 2];
			parameters[0] = new FunctionParameter(BasicTypeID.STRING, "name");
			parameters[1] = new FunctionParameter(BasicTypeID.INT, "ordinal");
			System.arraycopy(method.getHeader().parameters, 0, parameters, 2, parameterCount);
		} else {
			parameters = method.getHeader().parameters;
		}

		// In Java, the .ctor does not return the type, so let's override the return type
		final FunctionHeader javaHeader = new FunctionHeader(
				method.getHeader().typeParameters,
				BasicTypeID.VOID,
				method.getHeader().thrownType,
				parameters);


		return getMethodSignature(javaHeader, true, true);
	}
}
