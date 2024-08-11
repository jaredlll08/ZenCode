package org.openzen.zenscript.scripting;

import org.openzen.zencode.java.ZenCodeGlobals;
import org.openzen.zencode.java.ZenCodeType;

@ZenCodeType.Name(".globals")
public class Globals {
	@ZenCodeType.Method
	@ZenCodeGlobals.Global
	public static void println(String s) {
		System.out.println(s);
	}

	@ZenCodeType.Method
	@ZenCodeGlobals.Global
	public static void print(String s) {
		System.out.println(s);
	}
}
