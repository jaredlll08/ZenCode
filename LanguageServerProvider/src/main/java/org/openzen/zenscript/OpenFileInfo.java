package org.openzen.zenscript;

import org.eclipse.lsp4j.*;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zencode.shared.CompileException;
import org.openzen.zencode.shared.LiteralSourceFile;
import org.openzen.zencode.shared.VirtualSourceFile;
import org.openzen.zenscript.codemodel.*;
import org.openzen.zenscript.codemodel.compilation.CompileContext;
import org.openzen.zenscript.codemodel.compilation.CompilingDefinition;
import org.openzen.zenscript.codemodel.compilation.CompilingExpansion;
import org.openzen.zenscript.codemodel.compilation.StatementCompiler;
import org.openzen.zenscript.codemodel.compilation.statement.CompilingStatement;
import org.openzen.zenscript.codemodel.context.CompilingPackage;
import org.openzen.zenscript.codemodel.definition.ZSPackage;
import org.openzen.zenscript.codemodel.identifiers.ModuleSymbol;
import org.openzen.zenscript.codemodel.identifiers.TypeSymbol;
import org.openzen.zenscript.codemodel.ssa.CodeBlock;
import org.openzen.zenscript.codemodel.ssa.SSA;
import org.openzen.zenscript.codemodel.statement.Statement;
import org.openzen.zenscript.codemodel.type.BasicTypeID;
import org.openzen.zenscript.lexer.ParseException;
import org.openzen.zenscript.lexer.ZSToken;
import org.openzen.zenscript.lexer.ZSTokenParser;
import org.openzen.zenscript.parser.ParsedDefinition;
import org.openzen.zenscript.parser.ParsedFile;
import org.openzen.zenscript.parser.ParsedFileCompiler;
import org.openzen.zenscript.parser.ParsedImport;
import org.openzen.zenscript.parser.statements.ParsedStatement;
import org.openzen.zenscript.scripting.BasicBracketExpressionParser;
import org.openzen.zenscript.semantics.SemanticTokenParser;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class OpenFileInfo {
	public final TreeMap<CodePosition, ZSToken> tokensAtPosition = new TreeMap<>(Comparator.comparing(CodePosition::getFromLine).thenComparing(CodePosition::getFromLineOffset));
	public ParsedFile parsedFile;
	public PublishDiagnosticsParams diagnosticsParams;
	public String uri;

	private Optional<List<HighLevelDefinition>> definitions = Optional.empty();
	private Optional<ScriptBlock> scriptBlock = Optional.empty();
	private Optional<List<Statement>> statements = Optional.empty();

	public static OpenFileInfo createFrom(String text, String uri) {
		final OpenFileInfo result = new OpenFileInfo();

		final LiteralSourceFile sourceFile = new LiteralSourceFile(uri, text);
		final CompilingPackage compilingPackage = new CompilingPackage(new ZSPackage(null, "test"), new ModuleSymbol("test"));
		final List<ParseException> parseExceptions = new ArrayList<>();

		final ParsedFile parsedFile = getParsedFile(uri, sourceFile, compilingPackage, parseExceptions);
		final List<Diagnostic> diagnostics = getDiagnostics(parseExceptions);

		result.diagnosticsParams = new PublishDiagnosticsParams(uri, diagnostics);
		result.parsedFile = parsedFile;
		result.tokensAtPosition.putAll(getTokensAtPositions(text, uri));
		result.uri = uri;

		Logger.getGlobal().log(Level.FINEST, "Collected Tokens", result.tokensAtPosition);

		return result;
	}

	private static Map<CodePosition, ZSToken> getTokensAtPositions(String text, String uri) {
		try {
			final LiteralSourceFile sourceFile = new LiteralSourceFile(uri, text);
			final ZSTokenParser tokens = ZSTokenParser.create(sourceFile, null);
			return getTokens(tokens).collect(Collectors.toMap(Pair::first, Pair::second));
		} catch (IOException | ParseException exception) {
			Logger.getGlobal().log(Level.SEVERE, "Could not read tokenStream", exception);
			return Collections.emptyMap();
		}
	}

	private static List<Diagnostic> getDiagnostics(List<ParseException> parseExceptions) {
		final List<Diagnostic> diagnostics = new ArrayList<>();
		for (ParseException error : parseExceptions) {

			final Position rangeStart = new Position(error.position.fromLine - 1, error.position.fromLineOffset);
			final Position rangeEnd = new Position(error.position.toLine - 1, error.position.toLineOffset);
			if (rangeEnd.equals(rangeStart)) {
				rangeEnd.setCharacter(rangeEnd.getCharacter() + 1);
			}


			Logger.getGlobal().log(Level.FINEST, "Got Exception! (message, position)", new Object[]{error.message, error.position});

			final Range range = new Range(rangeStart, rangeEnd);

			final String message = error.message;
			final DiagnosticSeverity severity = DiagnosticSeverity.Error;
			final String source = "LSP";

			final Diagnostic diagnostic = new Diagnostic(range, message, severity, source);
			diagnostics.add(diagnostic);
		}
		return diagnostics;
	}

	private static ParsedFile getParsedFile(String uri, LiteralSourceFile sourceFile, CompilingPackage compilingPackage, List<ParseException> parseExceptions) {
		ParsedFile parsedFile;
		try {
			final ZSTokenParser tokens = ZSTokenParser.create(sourceFile, new BasicBracketExpressionParser());

			parsedFile = ParsedFile.parse(compilingPackage, tokens);
			parseExceptions.addAll(tokens.getErrors());
		} catch (ParseException | IOException e) {
			Logger.getGlobal().log(Level.WARNING, "Got exception while opening", e);
			parsedFile = new ParsedFile(compilingPackage, new VirtualSourceFile(uri));
			if (e instanceof ParseException) {
				parseExceptions.add((ParseException) e);
			}
		}
		parseExceptions.addAll(parsedFile.getErrors());
		return parsedFile;
	}

	private static Stream<Pair<CodePosition, ZSToken>> getTokens(ZSTokenParser parser) {
		final Iterator<Pair<CodePosition, ZSToken>> iterable = new Iterator<Pair<CodePosition, ZSToken>>() {
			private Pair<CodePosition, ZSToken> next;

			@Override
			public boolean hasNext() {
				return parser.hasNext() && moveNext();
			}

			private boolean moveNext() {
				try {
					final CodePosition positionStart = parser.getPosition();
					final ZSToken next = parser.next();
					final CodePosition positionEnd = parser.getPositionBeforeWhitespace();
					final CodePosition tokenPosition = positionStart.until(positionEnd);
					this.next = new Pair<>(tokenPosition, next);
					return true;
				} catch (ParseException e) {
					Logger.getGlobal().log(Level.WARNING, "Could not move to next token: ", e);
					return false;
				}
			}

			@Override
			public Pair<CodePosition, ZSToken> next() {
				return next;
			}
		};

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterable, Spliterator.ORDERED), false);
	}

	@SuppressWarnings("UnnecessaryLocalVariable")
	public static CodePosition positionToCodePosition(String uri, Position position) {
		final int fromLine = position.getLine() + 1;
		final int fromLineOffset = position.getCharacter() + 1;
		final int toLine = fromLine;
		final int toLineOffset = fromLineOffset + 1;
		return new CodePosition(new VirtualSourceFile(uri), fromLine, fromLineOffset, toLine, toLineOffset);
	}

	public static Range codePositionToRange(CodePosition codePosition) {

		final int startLine = codePosition.fromLine - 1;
		final int startLineOffset = codePosition.fromLineOffset;
		Position start = new Position(startLine, startLineOffset);


		final int endLine = codePosition.toLine - 1;
		final int endLineOffset = codePosition.toLineOffset;
		Position end = new Position(endLine, endLineOffset);
		return new Range(start, end);
	}

	public static Range codePositionsToRange(CodePosition startPos, CodePosition endPos) {

		final int startLine = startPos.fromLine - 1;
		final int startLineOffset = startPos.fromLineOffset;
		Position start = new Position(startLine, startLineOffset);


		final int endLine = endPos.toLine - 1;
		final int endLineOffset = endPos.toLineOffset;
		Position end = new Position(endLine, endLineOffset);
		return new Range(start, end);
	}

	public ZSTokenParser getTokenParser() {
		try {
			return ZSTokenParser.create(this.parsedFile.file, null);
		} catch (IOException | ParseException exception) {
			Logger.getGlobal().log(Level.SEVERE, "Could not read tokenStream", exception);
			return null; // TODO something
		}
	}

	public List<HighLevelDefinition> getDefinitions(ModuleSpace registry) {

		if (definitions.isPresent()) {
			return definitions.get();
		}
		List<CompilingDefinition> definitions = new ArrayList<>();
		List<CompilingExpansion> expansions = new ArrayList<>();

		CompileContext context = new CompileContext(
				registry.rootPackage,
				new ZSPackage(null, "test"),
				registry.collectExpansions(),
				registry.collectGlobals(),
				registry.getAnnotations());
		ParsedFileCompiler fileCompiler = new ParsedFileCompiler(context, this.parsedFile.pkg);

		for (ParsedDefinition definition : parsedFile.definitions()) {
			if (!definition.isExpansion()) {
				definition.registerCompiling(definitions, expansions, fileCompiler);
			}
		}

		for (CompilingDefinition definition : definitions) {
			definition.getPackage().addType(definition.getName(), definition);
		}
		for (CompilingDefinition definition : definitions) {
			if (!definition.isInner()) {
				definition.linkTypes();
			}
		}
		{
			List<CompileException> errors = new ArrayList<>();
			for (CompilingDefinition definition : definitions) {
				definition.prepareMembers(errors);
			}
			for (CompilingExpansion expansion : expansions) {
				expansion.prepareMembers(errors);
			}
		}
		this.definitions = Optional.of(definitions.stream().map(CompilingDefinition::getDefinition).collect(Collectors.toList()));
		return this.definitions.get();
	}

	public ScriptBlock getScriptBlock(ModuleSpace registry, CompilingPackage modulePackage) {

		List<CompilingDefinition> definitions = new ArrayList<>();
		List<CompilingExpansion> expansions = new ArrayList<>();

		CompileContext context = new CompileContext(
				registry.rootPackage,
				new ZSPackage(null, "test"),
				registry.collectExpansions(),
				registry.collectGlobals(),
				registry.getAnnotations());
		ParsedFileCompiler fileCompiler = new ParsedFileCompiler(context, this.parsedFile.pkg);

		for (ParsedDefinition definition : parsedFile.definitions()) {
			if (!definition.isExpansion()) {
				definition.registerCompiling(definitions, expansions, fileCompiler);
			}
		}

		for (CompilingDefinition definition : definitions) {
			definition.getPackage().addType(definition.getName(), definition);
		}
		for (CompilingDefinition definition : definitions) {
			if (!definition.isInner()) {
				definition.linkTypes();
			}
		}
		{
			List<CompileException> errors = new ArrayList<>();
			for (CompilingDefinition definition : definitions) {
				definition.prepareMembers(errors);
			}
			for (CompilingExpansion expansion : expansions) {
				expansion.prepareMembers(errors);
			}
		}
		for (ParsedImport import_ : parsedFile.imports()) {
			//TODO relative imports
			TypeSymbol type = registry.rootPackage.getImport(import_.getPath(), 0);
			fileCompiler.addImport(import_.getName(), type);
		}

		FunctionHeader scriptHeader = new FunctionHeader(BasicTypeID.VOID, FunctionParameter.NONE);
		StatementCompiler compiler = fileCompiler.forScripts(scriptHeader);
		CodeBlock start = new CodeBlock();
		CodeBlock codeBlock = start;

		List<CompilingStatement> statements = new ArrayList<>();
		for (ParsedStatement statement : parsedFile.statements()) {
			CompilingStatement compiling = statement.compile(compiler, codeBlock);
			codeBlock = compiling.getTail();
			statements.add(compiling);
		}

		SSA ssa = new SSA(start);
		ssa.compute();

		List<Statement> compiledStatements = new ArrayList<>();
		for (CompilingStatement statement : statements) {
			compiledStatements.add(statement.complete());
		}

		ScriptBlock block = new ScriptBlock(parsedFile.file, modulePackage.module, modulePackage.getPackage(), scriptHeader, compiledStatements);
		block.setTag(WhitespacePostComment.class, parsedFile.postComment());
		this.scriptBlock = Optional.of(block);
		return block;
	}

	public List<Statement> compileStatements(ModuleSpace registry) {

		if (this.statements.isPresent()) {
			return this.statements.get();
		}
		List<CompilingDefinition> definitions = new ArrayList<>();
		List<CompilingExpansion> expansions = new ArrayList<>();

		CompileContext context = new CompileContext(
				registry.rootPackage,
				new ZSPackage(null, "test"),
				registry.collectExpansions(),
				registry.collectGlobals(),
				registry.getAnnotations());
		ParsedFileCompiler fileCompiler = new ParsedFileCompiler(context, this.parsedFile.pkg);

		for (ParsedDefinition definition : parsedFile.definitions()) {
			if (!definition.isExpansion()) {
				definition.registerCompiling(definitions, expansions, fileCompiler);
			}
		}

		for (CompilingDefinition definition : definitions) {
			definition.getPackage().addType(definition.getName(), definition);
		}
		for (CompilingDefinition definition : definitions) {
			if (!definition.isInner()) {
				definition.linkTypes();
			}
		}
		{
			List<CompileException> errors = new ArrayList<>();
			for (CompilingDefinition definition : definitions) {
				definition.prepareMembers(errors);
			}
			for (CompilingExpansion expansion : expansions) {
				expansion.prepareMembers(errors);
			}
		}

		for (ParsedImport import_ : parsedFile.imports()) {
			//TODO relative imports
			TypeSymbol type = registry.rootPackage.getImport(import_.getPath(), 0);
			fileCompiler.addImport(import_.getName(), type);
		}

		FunctionHeader scriptHeader = new FunctionHeader(BasicTypeID.VOID, FunctionParameter.NONE);
		StatementCompiler compiler = fileCompiler.forScripts(scriptHeader);
		CodeBlock start = new CodeBlock();
		CodeBlock codeBlock = start;

		List<CompilingStatement> statements = new ArrayList<>();
		for (ParsedStatement statement : parsedFile.statements()) {
			CompilingStatement compiling = statement.compile(compiler, codeBlock);
			codeBlock = compiling.getTail();
			statements.add(compiling);
		}

		SSA ssa = new SSA(start);
		ssa.compute();

		List<Statement> compiledStatements = new ArrayList<>();
		for (CompilingStatement statement : statements) {
			compiledStatements.add(statement.complete());
		}
		this.statements = Optional.of(compiledStatements);
		return compiledStatements;
	}

	public List<DocumentHighlight> getHighlightOnPosition(Position position) {
		final CodePosition codePosition = positionToCodePosition(position);
		final ZSToken value = tokensAtPosition.lowerEntry(codePosition).getValue();

		//Logger.getGlobal().log(Level.FINEST, "Found " + value + " at " +position);

		final List<DocumentHighlight> result = new ArrayList<>();
		for (Map.Entry<CodePosition, ZSToken> codePositionZSTokenEntry : tokensAtPosition.entrySet()) {
			if (codePositionZSTokenEntry.getValue().equals(value)) {
				final CodePosition key = codePositionZSTokenEntry.getKey();
				final Range range = codePositionToRange(key);

				//Logger.getGlobal().log(Level.FINEST, "Adding highlight", range);
				result.add(new DocumentHighlight(range));
			}
		}
		return result;
	}

	private CodePosition positionToCodePosition(Position position) {
		return positionToCodePosition(uri, position);
	}

}
