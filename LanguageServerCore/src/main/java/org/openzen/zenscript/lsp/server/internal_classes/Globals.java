package org.openzen.zenscript.lsp.server.internal_classes;

import org.openzen.zencode.java.ZenCodeGlobals;
import org.openzen.zencode.java.ZenCodeType;

@ZenCodeType.Name(".globals")
public class Globals {
	@ZenCodeType.Method
	@ZenCodeGlobals.Global
	public static void println(String s) {
		System.out.println(s);
	}
}
