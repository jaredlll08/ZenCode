package org.openzen.zenscript.scriptingexample.tests.actual_test.strings;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openzen.zenscript.scriptingexample.tests.helpers.ScriptBuilder;
import org.openzen.zenscript.scriptingexample.tests.helpers.ZenCodeTest;

import java.util.Arrays;

import static org.openzen.zencode.shared.StringExpansion.unescape;

public class StdLibFunctions extends ZenCodeTest {

	@ParameterizedTest
	@CsvSource({
			"hello,h",
			"hello,z",
			"Abc,a",
			"abc,a"
	})
	public void containsChar(String string, char character) {
		ScriptBuilder.create()
				.add(String.format("println('%s' as char in \"%s\");", character, string))
				.execute(this);

		logger.assertPrintOutputSize(1);
		final boolean expected = string.contains("" + character);
		logger.assertPrintOutput(0, Boolean.toString(expected));
	}

	@ParameterizedTest
	@CsvSource({"hello,hell", "hello,world", "Abc,ab", "abc,ab"})
	public void containsString(String string, String needle) {
		ScriptBuilder.create()
				.add(String.format("println(\"%s\" in \"%s\");", needle, string))
				.execute(this);

		logger.assertPrintOutputSize(1);
		final boolean expected = string.contains(needle);
		logger.assertPrintOutput(0, Boolean.toString(expected));
	}

	@ParameterizedTest
	@CsvSource({"hello,h", "hello,z", "Abc,a", "abc,a"})
	public void indexOfChar(String string, char character) {
		ScriptBuilder.create()
				.add(String.format("println(\"%s\".indexOf('%s'));", string, character))
				.execute(this);

		logger.assertPrintOutputSize(1);
		final int expected = string.indexOf(character);
		logger.assertPrintOutput(0, (expected < 0) ? "null" : String.valueOf(expected));
	}

	@ParameterizedTest
	@CsvSource({"hello,hell", "hello,world", "Abc,ab", "abc,ab"})
	public void indexOfString(String string, String needle) {
		ScriptBuilder.create()
				.add(String.format("println(\"%s\".indexOf(\"%s\"));", string, needle))
				.execute(this);

		logger.assertPrintOutputSize(1);
		final int expected = string.indexOf(needle);
		logger.assertPrintOutput(0, (expected < 0) ? "null" : String.valueOf(expected));
	}

	@ParameterizedTest
	@CsvSource({"hello,h,0", "hello,z,0", "Abc,a,0", "abc,a,0", "hello,h,2", "hello,z,2", "Abc,a,2", "abc,a,2"})
	public void indexOfFromChar(String string, char character, int from) {
		ScriptBuilder.create()
				.add(String.format("println(\"%s\".indexOf('%s', %d));", string, character, from))
				.execute(this);

		logger.assertPrintOutputSize(1);
		final int expected = string.indexOf(character, from);
		logger.assertPrintOutput(0, (expected < 0) ? "null" : String.valueOf(expected));
	}

	@ParameterizedTest
	@CsvSource({"hello,hell,0", "hello,world,0", "Abc,ab,0", "abc,ab,0", "hello,hell,2", "hello,world,2", "Abc,ab,2", "abc,ab,2"})
	public void indexOfFromString(String string, String needle, int from) {
		ScriptBuilder.create()
				.add(String.format("println(\"%s\".indexOf(\"%s\", %d));", string, needle, from))
				.execute(this);

		logger.assertPrintOutputSize(1);
		final int expected = string.indexOf(needle, from);
		logger.assertPrintOutput(0, (expected < 0) ? "null" : String.valueOf(expected));
	}


	@ParameterizedTest
	@CsvSource({"hello,h", "hello,z", "Abc,a", "abc,a"})
	public void lastIndexOfChar(String string, char character) {
		ScriptBuilder.create()
				.add(String.format("println(\"%s\".lastIndexOf('%s'));", string, character))
				.execute(this);

		logger.assertPrintOutputSize(1);
		final int expected = string.lastIndexOf(character);
		logger.assertPrintOutput(0, (expected < 0) ? "null" : String.valueOf(expected));
	}

	@ParameterizedTest
	@CsvSource({"hello,hell", "hello,world", "Abc,ab", "abc,ab"})
	public void lastIndexOfString(String string, String needle) {
		ScriptBuilder.create()
				.add(String.format("println(\"%s\".lastIndexOf(\"%s\"));", string, needle))
				.execute(this);

		logger.assertPrintOutputSize(1);
		final int expected = string.lastIndexOf(needle);
		logger.assertPrintOutput(0, (expected < 0) ? "null" : String.valueOf(expected));
	}

	@ParameterizedTest
	@CsvSource({"hello,h,0", "hello,z,0", "Abc,a,0", "abc,a,0", "hello,h,2", "hello,z,2", "Abc,a,2", "abc,a,2"})
	public void lastIndexOfFromChar(String string, char character, int from) {
		ScriptBuilder.create()
				.add(String.format("println(\"%s\".lastIndexOf('%s', %d));", string, character, from))
				.execute(this);

		logger.assertPrintOutputSize(1);
		final int expected = string.lastIndexOf(character, from);
		logger.assertPrintOutput(0, (expected < 0) ? "null" : String.valueOf(expected));
	}

	@ParameterizedTest
	@CsvSource({"hello,hell,0", "hello,world,0", "Abc,ab,0", "abc,ab,0", "hello,hell,2", "hello,world,2", "Abc,ab,2", "abc,ab,2"})
	public void lastIndexOfFromString(String string, String needle, int from) {
		ScriptBuilder.create()
				.add(String.format("println(\"%s\".lastIndexOf(\"%s\", %d));", string, needle, from))
				.execute(this);

		logger.assertPrintOutputSize(1);
		final int expected = string.lastIndexOf(needle, from);
		logger.assertPrintOutput(0, (expected < 0) ? "null" : String.valueOf(expected));
	}

	@ParameterizedTest
	@CsvSource({"hello,e", "abc,a", "Abc,a"})
	public void splitChar(String string, char character) {
		ScriptBuilder.create()
				.add(String.format("for splitResult in  \"%s\".split('%s') println(splitResult);", string, character))
				.execute(this);

		final String[] split = string.split(String.format("[%s]", character));
		logger.assertPrintOutputSize(split.length);
		for (int i = 0; i < split.length; i++) {
			logger.assertPrintOutput(i, split[i]);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"   hello   ", "\\tthat\\n", "\\r\\nasd    "})
	public void trim(String string) {
		ScriptBuilder.create()
				.add(String.format("println(\"%s\".trim());", string))
				.execute(this);

		logger.assertPrintOutputSize(1);
		logger.assertPrintOutput(0, unescape('"' + string + '"').expect().trim());
	}

	@ParameterizedTest
	@CsvSource({"hello,hell", "hello,lo", "abc,a", "Abc,a"})
	public void startsWith(String string, String head) {
		ScriptBuilder.create()
				.add(String.format("println(\"%s\".startsWith(\"%s\"));", string, head))
				.execute(this);

		logger.assertPrintOutputSize(1);
		logger.assertPrintOutput(0, Boolean.toString(string.startsWith(head)));
	}

	@ParameterizedTest
	@CsvSource({"hello,hell", "hello,lo", "abc,c", "Abc,C"})
	public void endsWith(String string, String head) {
		ScriptBuilder.create()
				.add(String.format("println(\"%s\".endsWith(\"%s\"));", string, head))
				.execute(this);

		logger.assertPrintOutputSize(1);
		logger.assertPrintOutput(0, Boolean.toString(string.endsWith(head)));
	}

	@ParameterizedTest
	@CsvSource({",10,_", ",10, ", "a,10,b", "already_too_long,10,X"})
	public void lpad(String string, int length, Character filler) {
		if (filler == null) {
			filler = ' ';
		}
		if (string == null) {
			string = "";
		}

		ScriptBuilder.create()
				.add(String.format("println(\"%s\".lpad(%d,'%s'));", string, length, filler))
				.execute(this);

		int toFill = length - string.length();
		String paddedString = (toFill > 0) ? (makeString(toFill, filler) + string) : string;

		logger.assertPrintOutputSize(1);
		logger.assertPrintOutput(0, paddedString);
	}

	@ParameterizedTest
	@CsvSource({",10,_", ",10, ", "a,10,b", "already_too_long,10,X"})
	public void rpad(String string, int length, Character filler) {
		if (filler == null) {
			filler = ' ';
		}
		if (string == null) {
			string = "";
		}

		ScriptBuilder.create()
				.add(String.format("println(\"%s\".rpad(%d,'%s'));", string, length, filler))
				.execute(this);

		int toFill = length - string.length();
		String paddedString = (toFill > 0) ? (string + makeString(toFill, filler)) : string;

		logger.assertPrintOutputSize(1);
		logger.assertPrintOutput(0, paddedString);
	}

	private String makeString(int size, char filler) {
		final char[] chars = new char[size];
		Arrays.fill(chars, filler);
		return new String(chars);
	}
}
