package org.openzen.zenscript.javabytecode.compiler;

import org.openzen.zenscript.codemodel.type.*;
import org.openzen.zenscript.javashared.JavaClass;
import org.openzen.zenscript.javashared.JavaMethod;

public class JavaBoxingTypeVisitor implements TypeVisitorWithContext<StoredType, Void, RuntimeException> {
	private static final JavaMethod BOOLEAN_VALUEOF = JavaMethod.getNativeStatic(JavaClass.BOOLEAN, "valueOf", "(Z)Ljava/lang/Boolean;");
	private static final JavaMethod BYTE_VALUEOF = JavaMethod.getNativeStatic(JavaClass.BYTE, "valueOf", "(B)Ljava/lang/Byte;");
	private static final JavaMethod SHORT_VALUEOF = JavaMethod.getNativeStatic(JavaClass.SHORT, "valueOf", "(S)Ljava/lang/Short;");
	private static final JavaMethod INTEGER_VALUEOF = JavaMethod.getNativeStatic(JavaClass.INTEGER, "valueOf", "(I)Ljava/lang/Integer;");
	private static final JavaMethod LONG_VALUEOF = JavaMethod.getNativeStatic(JavaClass.LONG, "valueOf", "(J)Ljava/lang/Long;");
	private static final JavaMethod FLOAT_VALUEOF = JavaMethod.getNativeStatic(JavaClass.FLOAT, "valueOf", "(F)Ljava/lang/Float;");
	private static final JavaMethod DOUBLE_VALUEOF = JavaMethod.getNativeStatic(JavaClass.DOUBLE, "valueOf", "(D)Ljava/lang/Double;");
	private static final JavaMethod CHARACTER_VALUEOF = JavaMethod.getNativeStatic(JavaClass.CHARACTER, "valueOf", "(C)Ljava/lang/Character;");

	private final JavaWriter writer;

	public JavaBoxingTypeVisitor(JavaWriter writer) {
		this.writer = writer;
	}

	@Override
	public Void visitBasic(StoredType context, BasicTypeID basic) {
		final JavaMethod method;
		switch (basic) {
			case BOOL:
				method = BOOLEAN_VALUEOF;
				break;
			case BYTE:
				method = INTEGER_VALUEOF;
				break;
			case SBYTE:
				method = BYTE_VALUEOF;
				break;
			case SHORT:
				method = SHORT_VALUEOF;
				break;
			case USHORT:
				method = INTEGER_VALUEOF;
				break;
			case INT:
			case UINT:
			case USIZE:
				method = INTEGER_VALUEOF;
				break;
			case LONG:
			case ULONG:
				method = LONG_VALUEOF;
				break;
			case FLOAT:
				method = FLOAT_VALUEOF;
				break;
			case DOUBLE:
				method = DOUBLE_VALUEOF;
				break;
			case CHAR:
				method = CHARACTER_VALUEOF;
				break;
			default:
				return null;
		}
		
		if (method != null)
			writer.invokeStatic(method);
		return null;
	}
	
	@Override
	public Void visitString(StoredType context, StringTypeID string) {
		//NO-OP
		return null;
	}

	@Override
	public Void visitArray(StoredType context, ArrayTypeID array) {
		//NO-OP
		return null;
	}

	@Override
	public Void visitAssoc(StoredType context, AssocTypeID assoc) {
		//NO-OP
		return null;
	}

	@Override
	public Void visitGenericMap(StoredType context, GenericMapTypeID map) {
		//NO-OP
		return null;
	}

	@Override
	public Void visitIterator(StoredType context, IteratorTypeID iterator) {
		//NO-OP
		return null;
	}

	@Override
	public Void visitFunction(StoredType context, FunctionTypeID function) {
		//NO-OP
		return null;
	}

	@Override
	public Void visitDefinition(StoredType context, DefinitionTypeID definition) {
		//NO-OP
		return null;
	}

	@Override
	public Void visitGeneric(StoredType context, GenericTypeID generic) {
		//NO-OP
		return null;
	}

	@Override
	public Void visitRange(StoredType context, RangeTypeID range) {
		//NO-OP
		return null;
	}

	@Override
	public Void visitOptional(StoredType context, OptionalTypeID type) {
		//NO-OP
		return null;
	}
}
