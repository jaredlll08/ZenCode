package org.openzen.zenscript.lsp.server;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;
import org.openzen.zenscript.lsp.server.semantictokens.LSPSemanticTokenProvider;
import org.openzen.zenscript.lsp.server.zencode.ScriptingEngineProvider;
import org.openzen.zenscript.lsp.server.zencode.ScriptingEngineProviderImpl;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class OpenzenLSPServer implements LanguageServer, LanguageClientAware {
	private final TextDocumentService textDocumentService;
	private final WorkspaceService workspaceService;
	private final LSPSemanticTokenProvider semanticTokenProvider;

	@Nullable
	private LanguageClient client;

	public OpenzenLSPServer() {
		this(new ScriptingEngineProviderImpl());
	}

	public OpenzenLSPServer(ScriptingEngineProvider scriptingEngineProvider) {
		this.semanticTokenProvider = new LSPSemanticTokenProvider();
		this.textDocumentService = new OpenzenTextDocumentService(semanticTokenProvider, this, scriptingEngineProvider);
		this.workspaceService = new OpenzenWorkspaceService();
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		return CompletableFuture.supplyAsync(() -> {
			final ServerCapabilities serverCapabilities = new ServerCapabilities();
			final WorkspaceFoldersOptions workspaceFoldersOptions = new WorkspaceFoldersOptions();
			workspaceFoldersOptions.setChangeNotifications(true);
			final WorkspaceServerCapabilities workspaceServerCapabilities = new WorkspaceServerCapabilities(workspaceFoldersOptions);
			serverCapabilities.setWorkspace(workspaceServerCapabilities);

			serverCapabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
			serverCapabilities.setCompletionProvider(new CompletionOptions());
			serverCapabilities.setSemanticTokensProvider(semanticTokenProvider.getOptions());
			serverCapabilities.setDocumentHighlightProvider(true);
			serverCapabilities.setDefinitionProvider(true);
			final ServerInfo serverInfo = new ServerInfo("ZenCode LSP", "0.0.0");

			return new InitializeResult(serverCapabilities, serverInfo);
		});
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		this.client = null;
		return CompletableFuture.completedFuture(null);
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

	@Override
	public void connect(LanguageClient client) {
		this.client = client;
	}

	@Nullable
	public LanguageClient getClient() {
		return client;
	}
}
