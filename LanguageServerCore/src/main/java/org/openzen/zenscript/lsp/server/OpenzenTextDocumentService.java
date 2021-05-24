package org.openzen.zenscript.lsp.server;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.openzen.zencode.java.ScriptingEngine;
import org.openzen.zencode.java.module.JavaNativeModule;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zencode.shared.LiteralSourceFile;
import org.openzen.zencode.shared.SourceFile;
import org.openzen.zenscript.codemodel.HighLevelDefinition;
import org.openzen.zenscript.codemodel.ScriptBlock;
import org.openzen.zenscript.codemodel.SemanticModule;
import org.openzen.zenscript.codemodel.definition.*;
import org.openzen.zenscript.codemodel.statement.Statement;
import org.openzen.zenscript.codemodel.statement.VarStatement;
import org.openzen.zenscript.lsp.server.internal_classes.Globals;
import org.openzen.zenscript.lsp.server.local_variables.LocalVariableNameCollectionStatementVisitor;
import org.openzen.zenscript.lsp.server.semantictokens.LSPSemanticTokenProvider;
import org.openzen.zenscript.lsp.server.zencode.logging.DiagnosisLogger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class OpenzenTextDocumentService implements TextDocumentService {
	private final LSPSemanticTokenProvider semanticTokenProvider;
	private final OpenzenLSPServer openzenLSPServer;
	private final Map<String, OpenFileInfo> openFiles = new HashMap<>();

	private ScriptingEngine scriptingEngine;


	public OpenzenTextDocumentService(LSPSemanticTokenProvider semanticTokenProvider, OpenzenLSPServer openzenLSPServer) {
		this.semanticTokenProvider = semanticTokenProvider;
		this.openzenLSPServer = openzenLSPServer;
	}


	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		Logger.getGlobal().log(Level.FINER, "completion(CompletionParams position)");

		return CompletableFuture.supplyAsync(() -> {
			final List<CompletionItem> result = new ArrayList<>();
			final CompletionItem completionItem = new CompletionItem("some_label");
			completionItem.setDetail(position.getPosition().toString());
			completionItem.setKind(CompletionItemKind.Snippet);
			completionItem.setInsertText("public function helloWorld() {\n\tprintln('Hello World');\n}");
			result.add(completionItem);

			result.addAll(scriptingEngine.registry.getDefinitions()
					.stream()
					.map(id -> id.definition)
					.distinct()
					.map(this::convertDefinitionToCompletionItem)
					.filter(Objects::nonNull)
					.collect(Collectors.toList()));


			final HashSet<VarStatement> varStatements = new HashSet<>();
			final CodePosition queriedPosition = OpenFileInfo.positionToCodePosition(position.getTextDocument().getUri(), position.getPosition());
			final LocalVariableNameCollectionStatementVisitor visitor = new LocalVariableNameCollectionStatementVisitor(queriedPosition);

			for (SemanticModule compiledModule : scriptingEngine.getCompiledModules()) {
				for (ScriptBlock script : compiledModule.scripts) {
					for (Statement statement : script.statements) {
						statement.accept(varStatements, visitor);
					}
				}
			}
			result.addAll(varStatements.stream()
					.map(this::convertVarStatementToDefinition)
					.collect(Collectors.toList()));


			return Either.forLeft(result);
		});
	}

	private CompletionItem convertVarStatementToDefinition(VarStatement varStatement) {
		final CompletionItem completionItem = new CompletionItem(varStatement.name);
		completionItem.setKind(CompletionItemKind.Variable);
		completionItem.setDetail(varStatement.position.toShortString());

		return completionItem;
	}

	private CompletionItem convertDefinitionToCompletionItem(HighLevelDefinition definition) {
		if (definition.name == null) {//Removes Expansions from the list
			return null;
		}

		final CompletionItem completionItem = new CompletionItem(definition.getFullName());
		completionItem.setKind(getKindFromDefinition(definition));
		completionItem.setDetail(definition.position.toShortString());
		return completionItem;
	}

	private CompletionItemKind getKindFromDefinition(HighLevelDefinition definition) {
		if (definition instanceof EnumDefinition || definition instanceof VariantDefinition) {
			return CompletionItemKind.Enum;
		}
		if (definition instanceof FunctionDefinition) {
			return CompletionItemKind.Function;
		}
		if (definition instanceof InterfaceDefinition) {
			return CompletionItemKind.Interface;
		}
		if (definition instanceof StructDefinition) {
			return CompletionItemKind.Struct;
		}
		if (definition instanceof ExpansionDefinition) {
			return CompletionItemKind.Class;
		}
		if (definition instanceof AliasDefinition) {
			return CompletionItemKind.Reference;
		}
		if (definition instanceof ClassDefinition) {
			return CompletionItemKind.Class;
		}

		return null;
	}


	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		final TextDocumentItem textDocument = params.getTextDocument();
		parseAndUpdateCache(textDocument.getText(), textDocument.getUri());
	}

	private void parseAndUpdateCache(String text, String uri) {
		final OpenFileInfo from = OpenFileInfo.createFrom(text, uri);
		openFiles.put(uri, from);

		final DiagnosisLogger diagnosisLogger = new DiagnosisLogger();
		try {
			scriptingEngine = new ScriptingEngine(diagnosisLogger);
			scriptingEngine.debug = true;

			//We need these registered, since otherwise we cannot resolve e.g. globals
			//Well we'll need them for autocompletion anyways ^^
			//Later probably rather dynamic (like, we need them from the CrT registry somehow)
			final JavaNativeModule internal = scriptingEngine.createNativeModule("internal", "");
			internal.addGlobals(Globals.class);
			scriptingEngine.registerNativeProvided(internal);


			final SemanticModule lsp = scriptingEngine.createScriptedModule("lsp", new SourceFile[]{new LiteralSourceFile(uri, text)});
			scriptingEngine.registerCompiled(lsp);
		} catch (Exception e) {
			Logger.getGlobal().log(Level.WARNING, "Caught exception wenn reading StdLibs.jar", e);
		}

		final LanguageClient client = openzenLSPServer.getClient();
		if (client != null) {
			client.publishDiagnostics(diagnosisLogger.mergeDiagnosticParams(from.diagnosticsParams));
		}
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		final VersionedTextDocumentIdentifier textDocument = params.getTextDocument();
		final List<TextDocumentContentChangeEvent> contentChanges = params.getContentChanges();
		if (contentChanges.size() > 0) {
			parseAndUpdateCache(contentChanges.get(0).getText(), textDocument.getUri());
		}
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {

	}

	@Override
	public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
		return semanticTokenProvider.tokensFull(params);
	}

	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
		final OpenFileInfo openFileInfo = openFiles.get(params.getTextDocument().getUri());
		return CompletableFuture.supplyAsync(() -> openFileInfo.getHighlightOnPosition(params.getPosition()));
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
		Logger.getGlobal().log(Level.FINER, "definition(DefinitionParams params)");
		return CompletableFuture.supplyAsync(() -> {
			final String uri = params.getTextDocument().getUri();
			final Position start = new Position(1, 1);
			final Position end = new Position(1, 5);
			final Range range = new Range(start, end);
			final Location location = new Location(uri, range);

			return Either.forLeft(Collections.singletonList(location));
		});
	}
}
