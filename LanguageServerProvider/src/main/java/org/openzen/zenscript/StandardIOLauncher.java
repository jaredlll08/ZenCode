package org.openzen.zenscript;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

public class StandardIOLauncher {

    public static void main(String[] args) {
        start();
    }

    public static void start() {
        try {
            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(ZCLSPServer.INSTANCE, System.in, System.out);
            LanguageClient client = launcher.getRemoteProxy();
			ZCLSPServer.INSTANCE.connect(client);
            launcher.startListening().get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
