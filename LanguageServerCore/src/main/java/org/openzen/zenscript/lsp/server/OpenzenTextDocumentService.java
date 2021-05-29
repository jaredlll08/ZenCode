package org.openzen.zenscript.lsp.server;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.*;
import org.eclipse.lsp4j.services.*;
import org.openzen.zencode.java.*;
import org.openzen.zencode.shared.*;
import org.openzen.zenscript.codemodel.*;
import org.openzen.zenscript.codemodel.definition.*;
import org.openzen.zenscript.codemodel.expression.*;
import org.openzen.zenscript.codemodel.member.ref.*;
import org.openzen.zenscript.codemodel.statement.*;
import org.openzen.zenscript.codemodel.type.member.*;
import org.openzen.zenscript.lexer.*;
import org.openzen.zenscript.lsp.server.local_variables.*;
import org.openzen.zenscript.lsp.server.semantictokens.*;
import org.openzen.zenscript.lsp.server.zencode.*;
import org.openzen.zenscript.lsp.server.zencode.logging.*;
import org.openzen.zenscript.parser.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.*;

public class OpenzenTextDocumentService implements TextDocumentService {
	private static final Logger LOG = Logger.getGlobal();
	private final LSPSemanticTokenProvider semanticTokenProvider;
	private final OpenzenLSPServer openzenLSPServer;
	private final Map<String, OpenFileInfo> openFiles = new HashMap<>();
	private final ScriptingEngineProvider scriptingEngineProvider;
	private ScriptingEngine scriptingEngine;


	public OpenzenTextDocumentService(LSPSemanticTokenProvider semanticTokenProvider, OpenzenLSPServer openzenLSPServer, ScriptingEngineProvider scriptingEngineProvider) {
		this.semanticTokenProvider = semanticTokenProvider;
		this.openzenLSPServer = openzenLSPServer;
		this.scriptingEngineProvider = scriptingEngineProvider;
	}


	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		LOG.log(Level.FINER, "completion(CompletionParams position)");

		return CompletableFuture.supplyAsync(() -> {
			final CodePosition queriedPosition = OpenFileInfo.positionToCodePosition(position.getTextDocument().getUri(), position.getPosition());
			final Optional<List<CompletionItem>> completionItems = fromDot(position, queriedPosition);
			if (completionItems.isPresent()) {
				return Either.forLeft(completionItems.get());
			}


			final List<CompletionItem> result = new ArrayList<>();
			//result.add(getFunctionCompletionItem(position));

			result.addAll(getCompletionItemsFromDefinition());
			result.addAll(getCompletionItemsFromVariables(queriedPosition));


			return Either.forLeft(result);
		});
	}

	private List<CompletionItem> getCompletionItemsFromVariables(CodePosition queriedPosition) {
		final HashSet<VarStatement> varStatements = new HashSet<>();
		final LocalVariableNameCollectionStatementVisitor visitor = new LocalVariableNameCollectionStatementVisitor(queriedPosition);

		for (SemanticModule compiledModule : scriptingEngine.getCompiledModules()) {
			for (ScriptBlock script : compiledModule.scripts) {
				for (Statement statement : script.statements) {
					statement.accept(varStatements, visitor);
				}
			}
		}
		return varStatements.stream()
				.map(this::convertVarStatementToDefinition)
				.collect(Collectors.toList());
	}

	private List<CompletionItem> getCompletionItemsFromDefinition() {
		return scriptingEngine.registry.getDefinitions()
				.stream()
				.map(id -> id.definition)
				.distinct()
				.map(this::convertDefinitionToCompletionItem)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private CompletionItem getFunctionCompletionItem(CompletionParams position) {
		final CompletionItem completionItem = new CompletionItem("some_label");
		completionItem.setDetail(position.getPosition().toString());
		completionItem.setKind(CompletionItemKind.Snippet);
		completionItem.setInsertText("public function helloWorld() {\n\tprintln('Hello World');\n}");
		return completionItem;
	}

	private Optional<List<CompletionItem>> fromDot(CompletionParams position, CodePosition queriedPosition) {
		final OpenFileInfo openFileInfo = this.openFiles.get(position.getTextDocument().getUri());
		Map.Entry<CodePosition, ZSToken> codePositionZSTokenEntry = openFileInfo.tokensAtPosition.lowerEntry(queriedPosition);
		if (codePositionZSTokenEntry.getValue().type == ZSTokenType.T_IDENTIFIER) {
			//Already started typing -> x.yyy -> go up one step to x.
			codePositionZSTokenEntry = openFileInfo.tokensAtPosition.lowerEntry(codePositionZSTokenEntry.getKey());
		}

		if (codePositionZSTokenEntry.getValue().type != ZSTokenType.T_DOT) {
			return Optional.empty();
		}

		final Map.Entry<CodePosition, ZSToken> lowerEntry = openFileInfo.tokensAtPosition.lowerEntry(codePositionZSTokenEntry.getKey());
		LOG.log(Level.INFO, "Lower entry is", lowerEntry.getValue());
		final ExpressionFindingStatementVisitor exVisitor = new ExpressionFindingStatementVisitor(lowerEntry.getKey());
		boolean foundAny = false;
		final List<CompletionItem> result = new ArrayList<>();

		for (SemanticModule compiledModule : scriptingEngine.getCompiledModules()) {
			for (ScriptBlock script : compiledModule.scripts) {
				final Optional<Expression> expression = script.statements.stream().map(stmt -> stmt.accept(exVisitor))
						.filter(Optional::isPresent)
						.findAny()
						.flatMap(Function.identity());
				if (expression.isPresent()) {
					final LocalMemberCache localMemberCache = new LocalMemberCache(scriptingEngine.registry, Collections.emptyList());
					final TypeMembers typeMembers = localMemberCache.get(expression.get().type);
					for (String memberName : typeMembers.getMemberNames()) {
						final TypeMemberGroup group = typeMembers.getGroup(memberName);
						if (group.hasMethods()) {
							final TypeMember<FunctionalMemberRef> methodMember = group.getMethodMembers().get(0);
							final CompletionItem method = new CompletionItem(group.name);
							method.setKind(CompletionItemKind.Method);
							method.setDetail(methodMember.member.getHeader().getCanonical());
							result.add(method);
						} else if (group.getField() != null) {
							final CompletionItem field = new CompletionItem(group.name);
							field.setKind(CompletionItemKind.Field);
							field.setDetail(group.getField().member.getType().toString());
							result.add(field);
						} else if (group.getGetter() != null) {
							final CompletionItem getter = new CompletionItem(group.name);
							getter.setKind(CompletionItemKind.Field);
							getter.setDetail(group.getGetter().member.getType().toString());
							result.add(getter);
						} else {
							final CompletionItem unknown = new CompletionItem(group.name);
							unknown.setDetail("Unknown");
							result.add(unknown);
						}
					}
					foundAny = true;
				}
			}
		}

		if (foundAny) {
			return Optional.of(result);
		} else {
			return Optional.empty();
		}

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

		try {
			scriptingEngine = scriptingEngineProvider.createEngine();
			final BracketExpressionParser bracketExpressionParser = scriptingEngineProvider.getBracketExpressionParser();
			scriptingEngineProvider.initializeDefaultModules(scriptingEngine, bracketExpressionParser);

			final SourceFile[] sources = {new LiteralSourceFile(uri, text)};
			final FunctionParameter[] functionParameters = scriptingEngineProvider.getFunctionParameters();

			final SemanticModule lsp = scriptingEngine.createScriptedModule("lsp", sources, bracketExpressionParser, functionParameters);
			scriptingEngine.registerCompiled(lsp);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Caught exception wenn initializing engine or registering script", e);
		}

		final LanguageClient client = openzenLSPServer.getClient();
		if (client != null) {
			final DiagnosisLogger diagnosisLogger = (DiagnosisLogger) scriptingEngine.logger;
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
		LOG.log(Level.FINER, "definition(DefinitionParams params)");
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
