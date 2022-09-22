package org.openzen.zenscript.scriptingexample.tests.runner;

import org.junit.jupiter.api.Assertions;
import org.openzen.scriptingenginetester.TestOutput;
import org.openzen.scriptingenginetester.TestableScriptingEngine;
import org.openzen.scriptingenginetester.cases.TestCase;
import org.openzen.zencode.java.ScriptingEngine;
import org.openzen.zencode.java.module.JavaNativeModule;
import org.openzen.zencode.shared.CompileException;
import org.openzen.zencode.shared.SourceFile;
import org.openzen.zenscript.codemodel.FunctionParameter;
import org.openzen.zenscript.codemodel.SemanticModule;
import org.openzen.zenscript.lexer.ParseException;
import org.openzen.zenscript.scriptingexample.tests.SharedGlobals;
import org.openzen.zenscript.scriptingexample.tests.helpers.ZenCodeTestLogger;

import java.util.ArrayList;
import java.util.List;

public class JavaTestingEngineTest implements TestableScriptingEngine {
	public TestOutput run(TestCase test) {
		EngineTestLogger logger = new EngineTestLogger();
		ScriptingEngine engine = new ScriptingEngine(
				logger,
				ScriptingEngine.class::getResourceAsStream,
				test.getRequiredStdLibModules().toArray(new String[0])
		);
		engine.debug = true;
		JavaNativeModule testModule = engine.createNativeModule("testsupport", "org.openzen.zenscript.scripting_tests");
		SharedGlobals.currentlyActiveLogger = logger;

		getRequiredClasses().stream().distinct().forEach(requiredClass -> {
			testModule.addGlobals(requiredClass);
			testModule.addClass(requiredClass);
		});

		try {
			engine.registerNativeProvided(testModule);
		} catch (CompileException ex) {
			logger.logCompileException(ex);
		}
		run(engine, logger, test.getSourceFiles());
		return logger.getOutput();
	}

	private void run(ScriptingEngine engine, EngineTestLogger logger, List<SourceFile> sourceFiles) {
		try {
			final SemanticModule script_tests = engine.createScriptedModule("script_tests", sourceFiles
					.toArray(new SourceFile[0]), null, FunctionParameter.NONE, "testsupport");
			final boolean scriptsValid = script_tests.isValid();
			if (scriptsValid) {
				engine.registerCompiled(script_tests);
				engine.run();
			}
		} catch (ParseException e) {
			e.printStackTrace();
			Assertions.fail("Error in Engine execution", e);
		}
	}

	public List<Class<?>> getRequiredClasses() {
		final ArrayList<Class<?>> result = new ArrayList<>();
		result.add(SharedGlobals.class);
		return result;
	}
}
