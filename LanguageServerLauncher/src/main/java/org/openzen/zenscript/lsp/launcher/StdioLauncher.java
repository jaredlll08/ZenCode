package org.openzen.zenscript.lsp.launcher;

import java.io.*;
import java.util.logging.*;

/**
 * Simplest launcher.
 * Uses System in and System out to communicate with other processes
 */
public class StdioLauncher {
	public static void main(String[] args) {
		try {
			setupLogging();
			new StreamBasedLauncher(System.in, System.out).startServer();
		} catch (Throwable throwable) {
			Logger.getGlobal().log(Level.SEVERE, "Uncaught Error in Application", throwable);
		}
	}

	static void setupLogging() {
		removeGlobalLoggingHandlers();
		enableGlobalFileLogging();
		Logger.getGlobal().setLevel(Level.ALL);
	}

	private static void removeGlobalLoggingHandlers() {
		LogManager.getLogManager().reset();
		final Logger global = Logger.getGlobal();
		final Handler[] handlers = global.getHandlers();
		for (Handler handler : handlers) {
			global.removeHandler(handler);
		}
	}

	private static void enableGlobalFileLogging() {
		try {
			Logger.getGlobal().addHandler(new FileHandler("C:\\Dev\\Tweakers\\CraftTweaker_1.16\\ZenCode\\logging\\StdioLauncher.xml"));
		} catch (IOException exception) {
			Logger.getGlobal().log(Level.SEVERE, "Could not setup logger!", exception);
		}

	}
}
