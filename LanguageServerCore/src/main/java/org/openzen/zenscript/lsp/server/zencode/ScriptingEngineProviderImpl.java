package org.openzen.zenscript.lsp.server.zencode;

import org.openzen.zencode.java.*;
import org.openzen.zencode.java.module.*;
import org.openzen.zencode.shared.*;
import org.openzen.zenscript.codemodel.FunctionParameter;
import org.openzen.zenscript.lsp.server.internal_classes.*;
import org.openzen.zenscript.lsp.server.zencode.logging.DiagnosisLogger;
import org.openzen.zenscript.parser.*;

import java.util.logging.*;

public class ScriptingEngineProviderImpl implements ScriptingEngineProvider {
	private static final Logger LOG = Logger.getGlobal();

	@Override
	public ScriptingEngine createEngine() {
		final ScriptingEngine scriptingEngine = new ScriptingEngine(new DiagnosisLogger());
		scriptingEngine.debug = true;

		return scriptingEngine;
	}



	@Override
	public BracketExpressionParser getBracketExpressionParser() {
		return null;
	}

	@Override
	public void initializeDefaultModules(ScriptingEngine scriptingEngine, BracketExpressionParser parser) throws CompileException {
		//We need these registered, since otherwise we cannot resolve e.g. globals
		//Well we'll need them for autocompletion anyways ^^
		//Later probably rather dynamic (like, we need them from the CrT registry somehow)
		final JavaNativeModule internal = scriptingEngine.createNativeModule("internal", "");
		internal.addGlobals(Globals.class);
		scriptingEngine.registerNativeProvided(internal);
	}

	@Override
	public FunctionParameter[] getFunctionParameters() {
		return FunctionParameter.NONE;
	}
}
