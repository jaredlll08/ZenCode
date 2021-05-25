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
import org.openzen.zenscript.codemodel.expression.Expression;
import org.openzen.zenscript.codemodel.member.ref.FunctionalMemberRef;
import org.openzen.zenscript.codemodel.statement.Statement;
import org.openzen.zenscript.codemodel.statement.VarStatement;
import org.openzen.zenscript.codemodel.type.member.LocalMemberCache;
import org.openzen.zenscript.codemodel.type.member.TypeMember;
import org.openzen.zenscript.codemodel.type.member.TypeMemberGroup;
import org.openzen.zenscript.codemodel.type.member.TypeMembers;
import org.openzen.zenscript.lexer.ZSToken;
import org.openzen.zenscript.lexer.ZSTokenType;
import org.openzen.zenscript.lsp.server.internal_classes.Globals;
import org.openzen.zenscript.lsp.server.local_variables.ExpressionFindingStatementVisitor;
import org.openzen.zenscript.lsp.server.local_variables.LocalVariableNameCollectionStatementVisitor;
import org.openzen.zenscript.lsp.server.semantictokens.LSPSemanticTokenProvider;
import org.openzen.zenscript.lsp.server.zencode.logging.DiagnosisLogger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class OpenzenTextDocumentService implements TextDocumentService {
	private static final Logger LOG = Logger.getGlobal();
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
						} else if(group.getGetter() != null) {
							final CompletionItem getter = new CompletionItem(group.name);
							getter.setKind(CompletionItemKind.Field);
							getter.setDetail(group.getGetter().member.getType().toString());
							result.add(getter);
						}else {
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
			LOG.log(Level.WARNING, "Caught exception wenn reading StdLibs.jar", e);
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
