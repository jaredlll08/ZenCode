package org.openzen.zenscript.parser.definitions;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zencode.shared.CompileException;
import org.openzen.zenscript.codemodel.compilation.*;
import org.openzen.zenscript.codemodel.context.CompilingPackage;
import org.openzen.zenscript.codemodel.definition.FunctionDefinition;
import org.openzen.zenscript.codemodel.identifiers.TypeSymbol;
import org.openzen.zenscript.codemodel.type.BasicTypeID;
import org.openzen.zenscript.lexer.ParseException;
import org.openzen.zenscript.lexer.ZSTokenParser;
import org.openzen.zenscript.parser.ParsedAnnotation;
import org.openzen.zenscript.parser.ParsedDefinition;
import org.openzen.zenscript.parser.statements.ParsedFunctionBody;
import org.openzen.zenscript.parser.statements.ParsedStatement;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.openzen.zenscript.lexer.ZSTokenType.T_IDENTIFIER;

public class ParsedFunction extends ParsedDefinition {
	private final ParsedFunctionHeader header;
	private final ParsedFunctionBody body;
	private final String name;

	private ParsedFunction(CodePosition position, int modifiers, ParsedAnnotation[] annotations, String name, ParsedFunctionHeader header, ParsedFunctionBody body) {
		super(position, modifiers, annotations);

		this.header = header;
		this.body = body;
		this.name = name;
	}

	public static ParsedFunction parseFunction(
			CodePosition position,
			int modifiers,
			ParsedAnnotation[] annotations,
			ZSTokenParser parser
	) throws ParseException {
		String name = parser.required(T_IDENTIFIER, "identifier expected").content;
		ParsedFunctionHeader header = ParsedFunctionHeader.parse(parser);
		ParsedFunctionBody body = ParsedStatement.parseFunctionBody(parser);
		return new ParsedFunction(position, modifiers, annotations, name, header, body);
	}

	@Override
	public void registerCompiling(
		List<CompilingDefinition> definitions,
		List<CompilingExpansion> expansions,
		CompilingPackage pkg,
		DefinitionCompiler compiler,
		CompilingDefinition outer
	) {
		FunctionDefinition compiled = new FunctionDefinition(position, pkg.module, pkg.getPackage(), name, modifiers, outer == null ? null : outer.getDefinition());
		Compiling compiling = new Compiling(compiler, compiled, outer != null);
		definitions.add(compiling);
	}

	private class Compiling implements CompilingDefinition {
		private final DefinitionCompiler compiler;
		private final FunctionDefinition compiled;
		private final boolean inner;

		public Compiling(DefinitionCompiler compiler, FunctionDefinition compiled, boolean inner) {
			this.compiler = compiler;
			this.compiled = compiled;
			this.inner = inner;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public TypeSymbol getDefinition() {
			return compiled;
		}

		@Override
		public boolean isInner() {
			return inner;
		}

		@Override
		public void linkTypes() {
			if (compiled.header == null)
				compiled.setHeader(compiler.types(), header.compile(compiler.types()));
		}

		@Override
		public Set<TypeSymbol> getDependencies() {
			return null;
		}

		@Override
		public void prepareMembers(List<CompileException> errors) {

		}

		@Override
		public void compileMembers(List<CompileException> errors) {
			StatementCompiler compiler = this.compiler.forMembers(compiled).forMethod(compiled.header);
			compiled.setCode(body.compile(compiler));

			if (compiled.header.getReturnType() == BasicTypeID.UNDETERMINED)
				compiled.header.setReturnType(compiled.caller.body.getReturnType());
		}

		@Override
		public Optional<CompilingDefinition> getInner(String name) {
			return Optional.empty();
		}
	}
}
