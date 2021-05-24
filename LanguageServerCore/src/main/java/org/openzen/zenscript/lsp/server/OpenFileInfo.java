package org.openzen.zenscript.lsp.server;

import com.google.common.collect.Streams;
import org.eclipse.lsp4j.*;
import org.eclipse.xtext.xbase.lib.Pair;
import org.openzen.zencode.shared.*;
import org.openzen.zenscript.codemodel.*;
import org.openzen.zenscript.codemodel.context.*;
import org.openzen.zenscript.codemodel.definition.*;
import org.openzen.zenscript.lexer.*;
import org.openzen.zenscript.parser.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenFileInfo {
	public final TreeMap<CodePosition, ZSToken> tokensAtPosition = new TreeMap<>(Comparator.comparing(CodePosition::getFromLine).thenComparing(CodePosition::getFromLineOffset));
	public ParsedFile parsedFile;
	public PublishDiagnosticsParams diagnosticsParams;
	public String uri;

	public static OpenFileInfo createFrom(String text, String uri) {
		final OpenFileInfo result = new OpenFileInfo();

		final LiteralSourceFile sourceFile = new LiteralSourceFile(uri, text);
		final CompilingPackage compilingPackage = new CompilingPackage(new ZSPackage(null, "test"), new Module("test"));
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
			return getTokens(tokens).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
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
			final ZSTokenParser tokens = ZSTokenParser.create(sourceFile, null);

			parsedFile = ParsedFile.parse(compilingPackage, tokens);
			parseExceptions.addAll(tokens.getErrors());
			parseExceptions.addAll(parsedFile.getErrors());
		} catch (ParseException | IOException e) {
			Logger.getGlobal().log(Level.WARNING, "Got exception while opening", e);
			parsedFile = new ParsedFile(new VirtualSourceFile(uri));
			if (e instanceof ParseException) {
				parseExceptions.add((ParseException) e);
			}
		}
		parseExceptions.addAll(parsedFile.getErrors());
		return parsedFile;
	}

	private static Stream<Pair<CodePosition,ZSToken>> getTokens(ZSTokenParser parser) {
		final Iterator<Pair<CodePosition, ZSToken>> iterable = new Iterator<Pair<CodePosition,ZSToken>>() {
			private Pair<CodePosition,ZSToken> next;

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
			public Pair<CodePosition,ZSToken> next() {
				return next;
			}
		};

		//noinspection UnstableApiUsage
		return Streams.stream(iterable);
	}


	public List<DocumentHighlight> getHighlightOnPosition(Position position) {
		final CodePosition codePosition = positionToCodePosition(position);
		final ZSToken value = tokensAtPosition.lowerEntry(codePosition).getValue();

		//Logger.getGlobal().log(Level.FINEST, "Found " + value + " at " +position);

		final List<DocumentHighlight> result = new ArrayList<>();
		for (Map.Entry<CodePosition, ZSToken> codePositionZSTokenEntry : tokensAtPosition.entrySet()) {
			if(codePositionZSTokenEntry.getValue().equals(value)) {
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
}
