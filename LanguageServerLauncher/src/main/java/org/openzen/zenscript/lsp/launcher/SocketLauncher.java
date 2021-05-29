package org.openzen.zenscript.lsp.launcher;


import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class SocketLauncher {
	public static final int PORT = 6666;
	public static final Logger LOG = Logger.getGlobal();

	public static void main(String[] args) {

		logToFile();

		LOG.log(Level.INFO, "Listening on Port " + PORT);
		try (final ServerSocket serverSocket = new ServerSocket(PORT);
			 final Socket socket = serverSocket.accept()) {
			LOG.log(Level.INFO, "Connected.");
			new StreamBasedLauncher(socket.getInputStream(), socket.getOutputStream()).startServer();
		} catch (IOException | ExecutionException | InterruptedException e) {
			LOG.log(Level.WARNING, "Caught Exception", e);
		}
		LOG.log(Level.INFO, "Finished.");
	}

	private static void logToFile() {
		try {
			LOG.addHandler(new FileHandler("C:\\Dev\\Tweakers\\CraftTweaker_1.16\\ZenCode\\logging\\StdioLauncher.xml"));
		} catch (IOException exception) {
			LOG.log(Level.SEVERE, "Could not setup logger!", exception);
		}
	}
}
