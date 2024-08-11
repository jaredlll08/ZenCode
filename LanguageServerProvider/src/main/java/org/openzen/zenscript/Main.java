package org.openzen.zenscript;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

import java.io.*;
import java.net.Socket;
import java.util.Optional;

public final class Main {

	public static final String PATH = "X:\\LSP\\ZenCode\\LanguageServerProvider\\";

	private Main() {
	}

	public static Optional<Exception> launch(InputStream in, OutputStream out) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(PATH + "test.log"))) {
			ZCLSPServer server = new ZCLSPServer();
			Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out, false, new PrintWriter(writer));
			LanguageClient client = launcher.getRemoteProxy();

			server.connect(client);
			try {
				launcher.startListening().get();
				return Optional.empty();
			} catch (Exception e) {
				return Optional.of(e);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * @param args Arguments passed to launch server. First argument must either be
	 *             a port number for socket connection, or 0 to use STDIN and STDOUT
	 *             for communication
	 */
	public static void main(String[] args) {
		try {
			System.setErr(new PrintStream(PATH + "err.log"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Socket socket = null;
		InputStream in;
		OutputStream out;
		try {
			// If port is set to "0", use System.in/System.out.
			in = System.in;
			out = System.out;
			Optional<Exception> launchFailure = launch(in, out);

			if (launchFailure.isPresent()) {
				throw launchFailure.get();
			} else {
				System.out.println("Server terminated without errors");
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Missing port argument");
		} catch (NumberFormatException e) {
			System.out.println("Port number must be a valid integer");
		} catch (Exception e) {

			e.printStackTrace();
		} finally {
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (Exception e) {
				System.out.println("Failed to close the socket");
				e.printStackTrace();
			}
		}
	}
}