package org.openzen.zenscript.scripting;

import org.openzen.zencode.java.JavaNativeModuleBuilder;
import org.openzen.zencode.java.ScriptingEngine;
import org.openzen.zencode.shared.CompileException;
import org.openzen.zenscript.codemodel.FunctionParameter;
import org.openzen.zenscript.codemodel.SemanticModule;
import org.openzen.zenscript.codemodel.definition.ZSPackage;
import org.openzen.zenscript.json.JsonModule;
import org.openzen.zenscript.lexer.ParseException;
import org.openzen.zenscript.parser.ModuleLoader;
import org.openzen.zenscript.validator.Validator;

public class LSPEngine {
	private ScriptingEngine engine;

	public LSPEngine() {
		createEngine();
	}

	public void init() {
		//We need these registered, since otherwise we cannot resolve e.g. globals
		//Well we'll need them for autocompletion anyway ^^
		//Later probably rather dynamic (like, we need them from the CrT registry somehow)
		final JavaNativeModuleBuilder internal = engine.createNativeModule("internal", "");
		internal.addGlobals(Globals.class);
		try {
			engine.registerNativeProvided(internal.complete());
			registerModule(engine,"crafttweaker", engine.getRoot().getOrCreatePackage("crafttweaker"), new JsonModule.Loader());
		} catch (CompileException | ParseException e) {
			throw new RuntimeException(e);
		}


	}

	public void registerModule(ScriptingEngine engine, String name, ZSPackage zsPackage, ModuleLoader loader) throws CompileException, ParseException {
		SemanticModule stdlibModule = loader.loadModule(engine.space(), name, null, SemanticModule.NONE, FunctionParameter.NONE, zsPackage, engine.logger);
		stdlibModule = Validator.validate(stdlibModule, engine.logger);
		engine.space().addModule(name, stdlibModule);
		engine.registerCompiled(stdlibModule);
	}


	public ScriptingEngine engine() {
		return engine;
	}

	public void createEngine() {
		this.engine = new ScriptingEngine(new DiagnosisLogger(),
				s -> ScriptingEngine.class.getResourceAsStream(s.replaceAll("\\.jar", ".zip")));
	}

	public void engine(ScriptingEngine engine) {
		this.engine = engine;
	}
}
