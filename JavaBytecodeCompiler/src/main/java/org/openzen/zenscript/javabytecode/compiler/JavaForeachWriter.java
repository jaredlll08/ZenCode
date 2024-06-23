package org.openzen.zenscript.javabytecode.compiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.openzen.zenscript.codemodel.statement.ForeachStatement;
import org.openzen.zenscript.codemodel.type.ArrayTypeID;
import org.openzen.zenscript.codemodel.type.BasicTypeID;
import org.openzen.zenscript.codemodel.type.OptionalTypeID;
import org.openzen.zenscript.codemodel.type.RangeTypeID;
import org.openzen.zenscript.codemodel.type.TypeID;
import org.openzen.zenscript.javabytecode.JavaLocalVariableInfo;
import org.openzen.zenscript.javashared.JavaClass;
import org.openzen.zenscript.javashared.JavaNativeMethod;
import org.openzen.zenscript.javashared.JavaModifiers;

import java.util.Map;

@SuppressWarnings("Duplicates")
public class JavaForeachWriter {

	private final JavaWriter javaWriter;
	private final ForeachStatement statement;
	private final Label startOfLoopBody;
	private final Label endOfLoopBody;
	private final Label afterLoop;
	private final JavaStatementVisitor statementVisitor;
	private final JavaUnboxingTypeVisitor unboxingTypeVisitor;

	public JavaForeachWriter(JavaStatementVisitor statementVisitor, ForeachStatement statement, Label startOfLoopBody, Label endOfLoopBody, Label afterLoop) {
		this.statementVisitor = statementVisitor;
		this.javaWriter = statementVisitor.getJavaWriter();
		this.statement = statement;
		this.startOfLoopBody = startOfLoopBody;
		this.endOfLoopBody = endOfLoopBody;
		this.afterLoop = afterLoop;
		this.unboxingTypeVisitor = new JavaUnboxingTypeVisitor(this.javaWriter);
	}

	public void visitIntRange(RangeTypeID type) {
		final String owner = statementVisitor.context.getInternalName(type);
		javaWriter.dup();
		javaWriter.getField(owner, "to", "I");
		javaWriter.swap();
		javaWriter.getField(owner, "from", "I");

		final int z = javaWriter.getLocalVariable(statement.loopVariables[0].variable).local;
		javaWriter.storeInt(z);
		javaWriter.label(startOfLoopBody);
		javaWriter.dup();
		javaWriter.loadInt(z);
		javaWriter.ifICmpLE(afterLoop);

		statement.getContent().accept(statementVisitor);
		javaWriter.label(endOfLoopBody);
		javaWriter.iinc(z);
	}

	public void visitArrayValueIterator() {
		handleArray(javaWriter.local(int.class), javaWriter.getLocalVariable(statement.loopVariables[0].variable));
	}

	public void visitArrayKeyValueIterator() {
		handleArray(javaWriter.getLocalVariable(statement.loopVariables[0].variable).local, javaWriter.getLocalVariable(statement.loopVariables[1].variable));
	}

	public void visitStringCharacterIterator() {
		javaWriter.invokeVirtual(JavaNativeMethod.getVirtual(JavaClass.STRING, "toCharArray", "()[C", JavaModifiers.PUBLIC));
		handleArray(javaWriter.local(int.class), javaWriter.getLocalVariable(statement.loopVariables[0].variable));
	}

	public void visitIteratorIterator(Type targetType) {
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.ITERABLE, "iterator", "()Ljava/lang/Iterator;", 0));
		javaWriter.label(startOfLoopBody);
		javaWriter.dup();
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.ITERATOR, "hasNext", "()Z", 0));
		javaWriter.ifEQ(afterLoop);
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.ITERATOR, "next", "()Ljava/lang/Object;", 0, true));
		javaWriter.checkCast(targetType);
		final JavaLocalVariableInfo variable = javaWriter.getLocalVariable(statement.loopVariables[0].variable);
		javaWriter.store(variable.type, variable.local);

		statement.getContent().accept(statementVisitor);
		javaWriter.label(endOfLoopBody);
	}

	private void handleArray(final int z, final JavaLocalVariableInfo arrayTypeInfo) {
		if (statement.list.type.isOptional()) {
			javaWriter.dup();
			javaWriter.ifNull(afterLoop);
		}

		javaWriter.iConst0();
		javaWriter.storeInt(z);

		javaWriter.label(startOfLoopBody);
		javaWriter.dup();
		javaWriter.arrayLength();
		javaWriter.loadInt(z);

		javaWriter.ifICmpLE(afterLoop);
		javaWriter.dup();
		javaWriter.loadInt(z);

		ArrayTypeID listType = (ArrayTypeID) statement.list.type.withoutOptional();
 		if (listType.elementType == BasicTypeID.BYTE) {
			javaWriter.arrayLoad(Type.BYTE_TYPE);
			javaWriter.constant(0xFF);
			javaWriter.iAnd();
		} else if (listType.elementType == BasicTypeID.USHORT) {
			javaWriter.arrayLoad(Type.SHORT_TYPE);
			javaWriter.constant(0xFFFF);
			javaWriter.iAnd();
		} else {
			javaWriter.arrayLoad(arrayTypeInfo.type);
		}
		javaWriter.store(arrayTypeInfo.type, arrayTypeInfo.local);
		statement.getContent().accept(statementVisitor);

		javaWriter.label(endOfLoopBody);
		javaWriter.iinc(z);
	}

	public void visitCustomIterator() {
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(new JavaClass("java.lang", "Iterable", JavaClass.Kind.INTERFACE), "iterator", "()Ljava/util/Iterator;", 0));

		javaWriter.label(startOfLoopBody);
		javaWriter.dup();
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.ITERATOR, "hasNext", "()Z", 0));
		javaWriter.ifEQ(afterLoop);
		javaWriter.dup();
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.ITERATOR, "next", "()Ljava/lang/Object;", 0, true));

		final JavaLocalVariableInfo keyVariable = javaWriter.getLocalVariable(statement.loopVariables[0].variable);
		this.downCast(0, keyVariable.type);
		javaWriter.store(keyVariable.type, keyVariable.local);

		statement.getContent().accept(statementVisitor);
		javaWriter.label(endOfLoopBody);
	}

	public void visitAssocKeyIterator() {
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.MAP, "keySet", "()Ljava/util/Set;", 0));
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.COLLECTION, "iterator", "()Ljava/util/Iterator;", 0));

		javaWriter.label(startOfLoopBody);
		javaWriter.dup();
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.ITERATOR, "hasNext", "()Z", 0));
		javaWriter.ifEQ(afterLoop);
		javaWriter.dup();
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.ITERATOR, "next", "()Ljava/lang/Object;", 0, true));

		final JavaLocalVariableInfo keyVariable = javaWriter.getLocalVariable(statement.loopVariables[0].variable);
		this.downCast(0, keyVariable.type);
		javaWriter.store(keyVariable.type, keyVariable.local);

		statement.getContent().accept(statementVisitor);
		javaWriter.label(endOfLoopBody);
	}

	public void visitAssocKeyValueIterator() {
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.MAP, "entrySet", "()Ljava/util/Set;", 0));
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.COLLECTION, "iterator", "()Ljava/util/Iterator;", 0));

		javaWriter.label(startOfLoopBody);
		javaWriter.dup();
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.ITERATOR, "hasNext", "()Z", 0));
		javaWriter.ifEQ(afterLoop);
		javaWriter.dup();
		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.ITERATOR, "next", "()Ljava/lang/Object;", 0, true));
		javaWriter.checkCast(Type.getType(Map.Entry.class));
		javaWriter.dup(false);


		final JavaLocalVariableInfo keyVariable = javaWriter.getLocalVariable(statement.loopVariables[0].variable);
		final JavaLocalVariableInfo valueVariable = javaWriter.getLocalVariable(statement.loopVariables[1].variable);

		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.fromInternalName("java/util/Map$Entry", JavaClass.Kind.INTERFACE), "getKey", "()Ljava/lang/Object;", 0, true));
		this.downCast(0, keyVariable.type);
		javaWriter.store(keyVariable.type, keyVariable.local);

		javaWriter.invokeInterface(JavaNativeMethod.getVirtual(JavaClass.fromInternalName("java/util/Map$Entry", JavaClass.Kind.INTERFACE), "getValue", "()Ljava/lang/Object;", 0, true));
		this.downCast(1, valueVariable.type);
		javaWriter.store(valueVariable.type, valueVariable.local);

		statement.getContent().accept(statementVisitor);
		javaWriter.label(endOfLoopBody);
	}

	private void downCast(int typeNumber, Type t) {
		TypeID type = statement.loopVariables[typeNumber].type;
		if (CompilerUtils.isPrimitive(type)) {
			javaWriter.checkCast(statementVisitor.context.getInternalName(new OptionalTypeID(type)));
			type.accept(type, unboxingTypeVisitor);
		} else {
			javaWriter.checkCast(t);
		}
	}
}
