package org.openzen.zenscript.lsp.server.zencode;

import org.openzen.zencode.java.ScriptingEngine;
import org.openzen.zencode.shared.CompileException;
import org.openzen.zenscript.codemodel.FunctionParameter;
import org.openzen.zenscript.parser.BracketExpressionParser;

public interface ScriptingEngineProvider {

	ScriptingEngine createEngine();

	BracketExpressionParser getBracketExpressionParser();

	void initializeDefaultModules(ScriptingEngine scriptingEngine, BracketExpressionParser parser) throws CompileException;

	FunctionParameter[] getFunctionParameters();
}
