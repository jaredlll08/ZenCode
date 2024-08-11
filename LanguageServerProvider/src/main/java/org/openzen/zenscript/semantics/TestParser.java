package org.openzen.zenscript.semantics;

import org.openzen.zenscript.codemodel.statement.*;

import java.util.ArrayList;
import java.util.List;

public class TestParser implements StatementVisitor<Void> {

	public List<SemanticToken> tokens = new ArrayList<>();

	@Override
	public Void visitBlock(BlockStatement statement) {
		return null;
	}

	@Override
	public Void visitBreak(BreakStatement statement) {
		return null;
	}

	@Override
	public Void visitContinue(ContinueStatement statement) {
		return null;
	}

	@Override
	public Void visitDoWhile(DoWhileStatement statement) {
		return null;
	}

	@Override
	public Void visitEmpty(EmptyStatement statement) {
		return null;
	}

	@Override
	public Void visitExpression(ExpressionStatement statement) {
		return null;
	}

	@Override
	public Void visitForeach(ForeachStatement statement) {
		return null;
	}

	@Override
	public Void visitIf(IfStatement statement) {
		return null;
	}

	@Override
	public Void visitLock(LockStatement statement) {
		return null;
	}

	@Override
	public Void visitReturn(ReturnStatement statement) {
		return null;
	}

	@Override
	public Void visitSwitch(SwitchStatement statement) {
		return null;
	}

	@Override
	public Void visitThrow(ThrowStatement statement) {
		return null;
	}

	@Override
	public Void visitTryCatch(TryCatchStatement statement) {
		return null;
	}

	@Override
	public Void visitVar(VarStatement statement) {
		return null;
	}

	@Override
	public Void visitWhile(WhileStatement statement) {
		return null;
	}
}
