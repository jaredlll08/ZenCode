package org.openzen.zenscript;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zencode.shared.LiteralSourceFile;
import org.openzen.zencode.shared.SourceFile;
import org.openzen.zenscript.codemodel.FunctionParameter;
import org.openzen.zenscript.codemodel.HighLevelDefinition;
import org.openzen.zenscript.codemodel.ScriptBlock;
import org.openzen.zenscript.codemodel.SemanticModule;
import org.openzen.zenscript.codemodel.compilation.InstanceCallableMethod;
import org.openzen.zenscript.codemodel.compilation.ResolvedType;
import org.openzen.zenscript.codemodel.context.CompilingPackage;
import org.openzen.zenscript.codemodel.definition.*;
import org.openzen.zenscript.codemodel.expression.Expression;
import org.openzen.zenscript.codemodel.identifiers.MethodSymbol;
import org.openzen.zenscript.codemodel.identifiers.ModuleSymbol;
import org.openzen.zenscript.codemodel.identifiers.instances.MethodInstance;
import org.openzen.zenscript.codemodel.member.GetterMember;
import org.openzen.zenscript.codemodel.member.IDefinitionMember;
import org.openzen.zenscript.codemodel.member.MethodMember;
import org.openzen.zenscript.codemodel.statement.Statement;
import org.openzen.zenscript.codemodel.statement.VarStatement;
import org.openzen.zenscript.codemodel.type.TypeID;
import org.openzen.zenscript.diagnostics.DiagnosticsStatementVisitor;
import org.openzen.zenscript.formatter.FileFormatter;
import org.openzen.zenscript.formatter.ScriptFormattingSettings;
import org.openzen.zenscript.lexer.*;
import org.openzen.zenscript.parser.BracketExpressionParser;
import org.openzen.zenscript.parser.PositionalTokenParser;
import org.openzen.zenscript.parser.PositionedToken;
import org.openzen.zenscript.scripting.BasicBracketExpressionParser;
import org.openzen.zenscript.scripting.DiagnosisLogger;
import org.openzen.zenscript.scripting.LSPEngine;
import org.openzen.zenscript.scripting.visitor.ExpressionFindingStatementVisitor;
import org.openzen.zenscript.scripting.visitor.LocalVariableNameCollectionStatementVisitor;
import org.openzen.zenscript.scripting.visitor.completion.CompletionMemberVisitor;
import org.openzen.zenscript.scripting.visitor.completion.DotCompletionVisitor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ZCTextDocumentService implements TextDocumentService {

	private final ZCLSPServer server;
	private final LSPEngine engine;
	private final Map<String, OpenFileInfo> openFiles = new HashMap<>();

	public ZCTextDocumentService(ZCLSPServer server) {
		this.server = server;
		this.engine = new LSPEngine();
		this.engine.init();
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		return CompletableFuture.supplyAsync(() -> {
			final CodePosition queriedPosition = OpenFileInfo.positionToCodePosition(position.getTextDocument().getUri(), position.getPosition());
			final Optional<List<CompletionItem>> completionItems = fromDot(position, queriedPosition);
			if (completionItems.isPresent()) {
				return Either.forLeft(completionItems.get());
			}

			final List<CompletionItem> result = new ArrayList<>();
//			result.add(getFunctionCompletionItem(position));

			result.addAll(getCompletionItemsFromDefinition());
			result.addAll(getCompletionItemsFromVariables(queriedPosition));

			return Either.forLeft(result);
		});
	}


	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
		return CompletableFuture.completedFuture(new ArrayList<>());
	}

	@Override
	public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {

		return CompletableFuture.supplyAsync(() -> {

			OpenFileInfo file = openFiles.get(params.getTextDocument().getUri());
			List<Integer> data = new ArrayList<>();
//			ZCSemanticTokens.basicTokens(file, data);
//			ZCSemanticTokens.advancedTokens(engine, file, data);
			ZCSemanticTokens.positionedTokens(engine, file, data);

			return new SemanticTokens(data);
		});
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		return CompletableFuture.supplyAsync(() -> {

			OpenFileInfo file = openFiles.get(params.getTextDocument().getUri());
			try {
				PositionalTokenParser<ZSToken, ZSTokenType> raw = PositionalTokenParser.create(file.parsedFile.file, new ReaderCharReader(file.parsedFile.file.open()));
				List<PositionedToken<ZSTokenType, ZSToken>> tokens = new ArrayList<>();
				while(raw.hasNext()) {
					PositionedToken<ZSTokenType, ZSToken> next = raw.next();
					tokens.add(next);
				}
				server.client().ifPresent(languageClient -> {
					languageClient.logMessage(new MessageParams(MessageType.Info, tokens.toString()));
				});
			} catch (IOException | ParseException e) {
				throw new RuntimeException(e);
			}
			return Collections.emptyList();
//			FileFormatter fileFormatter = new FileFormatter(new ScriptFormattingSettings.Builder().classBracketOnSameLine(true).useSingleQuotesForStrings(false).build());
//			CompilingPackage compilingPackage = new CompilingPackage(new ZSPackage(null, "test"), new ModuleSymbol("test"));
//			String format = fileFormatter.format(file.parsedFile.pkg.getPackage(), file.getScriptBlock(engine.engine().space(), compilingPackage), file.getDefinitions(engine.engine().space()));
//			return Collections.singletonList(new TextEdit(OpenFileInfo.codePositionsToRange(file.tokensAtPosition.firstKey(), file.tokensAtPosition.lastKey()), format));
//			FormattingParser formattingParser = new FormattingParser();
//			formattingParser.parse(file.getTokenParser());
//			return Collections.singletonList(new TextEdit(OpenFileInfo.codePositionsToRange(file.tokensAtPosition.firstKey(), file.tokensAtPosition.lastKey()), String.join("\n", formattingParser.tokens())));
		});
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		return CompletableFuture.supplyAsync(() -> unresolved);
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
			engine.createEngine();
			final BracketExpressionParser bracketExpressionParser = new BasicBracketExpressionParser();
			engine.init();

			final SourceFile[] sources = {new LiteralSourceFile(uri, text)};

			final SemanticModule lsp = engine.engine().createScriptedModule("lsp", sources, bracketExpressionParser, FunctionParameter.NONE);
			engine.engine().registerCompiled(lsp);
		} catch (Exception e) {
			e.printStackTrace();
		}

		server.client().ifPresent(languageClient -> {
			final DiagnosisLogger diagnosisLogger = (DiagnosisLogger) engine.engine().logger;
			languageClient.publishDiagnostics(diagnosisLogger.mergeDiagnosticParams(from.diagnosticsParams));
		});
	}

	@Override
	public CompletableFuture<DocumentDiagnosticReport> diagnostic(DocumentDiagnosticParams params) {
		final OpenFileInfo openFileInfo = this.openFiles.get(params.getTextDocument().getUri());
		DiagnosisLogger logger = (DiagnosisLogger) engine.engine().logger;
		List<Statement> statements = openFileInfo.compileStatements(engine.engine().space());

		List<Diagnostic> diagnostics = new ArrayList<>();
		for (Statement statement : statements) {
			statement.accept(new DiagnosticsStatementVisitor(diagnostics));
		}

		PublishDiagnosticsParams merged = logger.mergeDiagnosticParams(openFileInfo.uri, diagnostics);
		return CompletableFuture.completedFuture(new DocumentDiagnosticReport(new RelatedFullDocumentDiagnosticReport(merged.getDiagnostics())));
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

	private List<HighLevelDefinition> getHighLevelDefinitions() {
		return this.engine.engine()
				.getCompiledModules()
				.stream()
				.flatMap(semanticModule -> semanticModule.definitions.getAll().stream()).collect(Collectors.toList());
	}

	private List<CompletionItem> getCompletionItemsFromDefinition() {
		return getHighLevelDefinitions()
				.stream()
				.map(this::convertDefinitionToCompletionItem)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
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


	private List<CompletionItem> getCompletionItemsFromVariables(CodePosition queriedPosition) {
		final HashSet<VarStatement> varStatements = new HashSet<>();
		final LocalVariableNameCollectionStatementVisitor visitor = new LocalVariableNameCollectionStatementVisitor(queriedPosition);

		for (SemanticModule compiledModule : engine.engine().getCompiledModules()) {
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

	private CompletionItem convertVarStatementToDefinition(VarStatement varStatement) {
		final CompletionItem completionItem = new CompletionItem(varStatement.name);
		completionItem.setKind(CompletionItemKind.Variable);
		completionItem.setDetail(varStatement.position.toShortString());

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
		final ExpressionFindingStatementVisitor exVisitor = new ExpressionFindingStatementVisitor(lowerEntry.getKey());
		boolean foundAny = false;
		final List<CompletionItem> result = new ArrayList<>();

		if (lowerEntry.getValue().getType() == ZSTokenType.T_IDENTIFIER) {
			String content = lowerEntry.getValue().getContent();
			List<Statement> statements = openFileInfo.compileStatements(engine.engine().space());

			for (Statement statement : statements) {
				Optional<TypeID> foundType = statement.accept(content, new DotCompletionVisitor());
				if (foundType.isPresent()) {
					TypeID typeID = foundType.get();
					ResolvedType resolve = typeID.resolve();
					for (InstanceCallableMethod instanceMethod : resolve.instanceMethods()) {
						if (instanceMethod instanceof MethodInstance) {
							MethodSymbol method = ((MethodInstance) instanceMethod).method;
							if (method.getID().name() != null) {
								if (method instanceof MethodMember) {
									CompletionItem completionItem = new CompletionItem();
									completionItem.setLabel(method.getID().name() + RenderingTypeVisitor.renderFunctionHeader(method.getHeader()));
									completionItem.setKind(CompletionItemKind.Method);
									completionItem.setDetail("detail");
									completionItem.setDocumentation(method.getHeader().toString());
									result.add(completionItem);
								}
								if (method instanceof GetterMember) {
									CompletionItem completionItem = new CompletionItem();
									completionItem.setLabel(method.getID().name());
									completionItem.setKind(CompletionItemKind.Field);
									completionItem.setDetail("detail");
									completionItem.setDocumentation(method.getHeader().toString());
									result.add(completionItem);
								}

								foundAny = true;
							}
						}
					}
					List<ExpansionDefinition> expansions = engine.engine().space().collectExpansions();
					for (ExpansionDefinition expansion : expansions) {
						if (expansion.target.equals(typeID)) {
							for (IDefinitionMember member : expansion.members) {
								Optional<CompletionItem> foundItem = member.accept(new CompletionMemberVisitor());
								foundItem.ifPresent(result::add);
								if (foundItem.isPresent()) {
									foundAny = true;
								}
							}
						}
					}
				}
			}
		}
		for (SemanticModule compiledModule : engine.engine().getCompiledModules()) {
//			for (ExpansionDefinition expansion : compiledModule.expansions) {
//				for (IDefinitionMember member : expansion.members) {
//					Optional<CompletionItem> accept = member.accept(new CompletionMemberVisitor());
//					accept.ifPresent(result::add);
//					if (accept.isPresent()) {
//						foundAny = true;
//					}
//				}
//			}
			for (ScriptBlock script : compiledModule.scripts) {
				final Optional<Expression> expression = script.statements.stream().map(stmt -> stmt.accept(exVisitor))
						.filter(Optional::isPresent)
						.findAny()
						.flatMap(Function.identity());
				if (expression.isPresent()) {
					ZCLSPServer.log("got expression");
//					final LocalMemberCache localMemberCache = new LocalMemberCache(engine.engine().registry, Collections.emptyList());
//					final TypeMembers typeMembers = localMemberCache.get(expression.get().type);
//					for (String memberName : typeMembers.getMemberNames()) {
//						final TypeMemberGroup group = typeMembers.getGroup(memberName);
//						if (group.hasMethods()) {
//							final TypeMember<FunctionalMemberRef> methodMember = group.getMethodMembers().get(0);
//							final CompletionItem method = new CompletionItem(group.name);
//							method.setKind(CompletionItemKind.Method);
//							method.setDetail(methodMember.member.getHeader().getCanonical());
//							result.add(method);
//						} else if (group.getField() != null) {
//							final CompletionItem field = new CompletionItem(group.name);
//							field.setKind(CompletionItemKind.Field);
//							field.setDetail(group.getField().member.getType().toString());
//							result.add(field);
//						} else if (group.getGetter() != null) {
//							final CompletionItem getter = new CompletionItem(group.name);
//							getter.setKind(CompletionItemKind.Field);
//							getter.setDetail(group.getGetter().member.getType().toString());
//							result.add(getter);
//						} else {
//							final CompletionItem unknown = new CompletionItem(group.name);
//							unknown.setDetail("Unknown");
//							result.add(unknown);
//						}
//					}
//					foundAny = true;
				}
			}
		}

		if (foundAny) {
			return Optional.of(result);
		} else {
			return Optional.empty();
		}

	}

}
