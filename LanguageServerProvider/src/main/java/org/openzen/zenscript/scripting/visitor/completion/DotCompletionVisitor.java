package org.openzen.zenscript.scripting.visitor.completion;

import org.eclipse.lsp4j.CompletionItem;
import org.openzen.zenscript.codemodel.statement.*;
import org.openzen.zenscript.codemodel.type.TypeID;

import java.util.Optional;

public class DotCompletionVisitor implements StatementVisitorWithContext<String, Optional<TypeID>> {

	@Override
	public Optional<TypeID> visitBlock(String context, BlockStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitBreak(String context, BreakStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitContinue(String context, ContinueStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitDoWhile(String context, DoWhileStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitEmpty(String context, EmptyStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitExpression(String context, ExpressionStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitForeach(String context, ForeachStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitIf(String context, IfStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitLock(String context, LockStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitReturn(String context, ReturnStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitSwitch(String context, SwitchStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitThrow(String context, ThrowStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitTryCatch(String context, TryCatchStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitVar(String context, VarStatement statement) {
		if(context.equals(statement.name)){
			return Optional.ofNullable(statement.type);
		}
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitWhile(String context, WhileStatement statement) {
		return Optional.empty();
	}

	@Override
	public Optional<TypeID> visitInvalid(String context, InvalidStatement statement) {
		return Optional.empty();
	}
}
