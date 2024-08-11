package org.openzen.zenscript;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ZCLSPServer implements LanguageServer, LanguageClientAware {

	private LanguageClient client;
	private WorkspaceService workspaceService;
	private TextDocumentService textDocumentService;

	public ZCLSPServer() {
		this.workspaceService = new ZCWorkspaceService();
		this.textDocumentService = new ZCTextDocumentService(this);
	}

	@Override
	public void connect(LanguageClient client) {
		this.client = client;
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		final ServerCapabilities serverCapabilities = new ServerCapabilities();
		final WorkspaceFoldersOptions workspaceFoldersOptions = new WorkspaceFoldersOptions();
		workspaceFoldersOptions.setChangeNotifications(true);
		final WorkspaceServerCapabilities workspaceServerCapabilities = new WorkspaceServerCapabilities(workspaceFoldersOptions);
		serverCapabilities.setWorkspace(workspaceServerCapabilities);
		serverCapabilities.setDocumentFormattingProvider(true);
		serverCapabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
		serverCapabilities.setCompletionProvider(new CompletionOptions(true, Collections.singletonList(".")));
		serverCapabilities.setSemanticTokensProvider(new SemanticTokensWithRegistrationOptions(ZCSemanticTokens.SEMANTIC_TOKENS_LEGEND, true));
		serverCapabilities.setDiagnosticProvider(new DiagnosticRegistrationOptions());
//		serverCapabilities.setDocumentHighlightProvider(true);
//		serverCapabilities.setDefinitionProvider(true);
		final ServerInfo serverInfo = new ServerInfo("ZenCode LSP", "0.0.0");

		return CompletableFuture.supplyAsync(() -> new InitializeResult(serverCapabilities, serverInfo));
	}

	@Override
	public CompletableFuture<Object> shutdown() {
//		this.client = null;
		return CompletableFuture.supplyAsync(Object::new);
	}

	@Override
	public void exit() {
		System.exit(client == null ? 0 : 1);
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return textDocumentService;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return workspaceService;
	}

	public Optional<LanguageClient> client() {
		return Optional.ofNullable(client);
	}

	@Override
	public void setTrace(SetTraceParams params) {
		//TODO
	}
}
