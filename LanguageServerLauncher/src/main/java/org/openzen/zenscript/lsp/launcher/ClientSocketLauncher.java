package org.openzen.zenscript.lsp.launcher;

import java.io.*;
import java.net.*;
import java.util.logging.*;

public class ClientSocketLauncher {
	public static final int PORT = 6666;
	private static final Logger LOG = Logger.getGlobal();

	public static void main(String[] args) {
		StdioLauncher.setupLogging();
		try (final Socket socket = new Socket(InetAddress.getLocalHost(), PORT);
			 final StreamRedirection inToSocket = new StreamRedirection(System.in, socket.getOutputStream());
			 final StreamRedirection socketToOut = new StreamRedirection(socket.getInputStream(), System.out)) {
			inToSocket.start();
			socketToOut.start();
			socketToOut.join();
		} catch (IOException | InterruptedException e) {
			LOG.log(Level.SEVERE, "Caught Exception", e);
		}


	}
}
