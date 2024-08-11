package org.openzen.zencode.shared;

import java.util.List;
import java.util.Objects;

public final class CodePosition implements Comparable<CodePosition> {
	public static final CodePosition BUILTIN = new CodePosition(new VirtualSourceFile("builtin"), 0, 0, 0, 0);
	public static final CodePosition NATIVE = new CodePosition(new VirtualSourceFile("native"), 0, 0, 0, 0);
	public static final CodePosition META = new CodePosition(new VirtualSourceFile("meta"), 0, 0, 0, 0);
	public static final CodePosition UNKNOWN = new CodePosition(new VirtualSourceFile("unknown"), 0, 0, 0, 0);
	public static final CodePosition GENERATED = new CodePosition(new VirtualSourceFile("generated"), 0, 0, 0, 0);
	public final SourceFile file;
	public final int fromLine;
	public final int fromLineOffset;
	public final int toLine;
	public final int toLineOffset;

	public CodePosition(SourceFile file, int fromLine, int fromLineOffset, int toLine, int toLineOffset) {
		this.file = file;
		this.fromLine = fromLine;
		this.fromLineOffset = fromLineOffset;
		this.toLine = toLine;
		this.toLineOffset = toLineOffset;
	}

	public String getFilename() {
		return file.getFilename();
	}

	public String toShortString() {
		List<String> filePath = file.getFilePath();
		String shortFilename = filePath.get(filePath.size() - 1);
		if (fromLine == 0 && fromLineOffset == 0)
			return shortFilename;
		return shortFilename + ":" + fromLine + ":" + fromLineOffset;
	}

	public String toLongString() {
		return this + "~" + toLine + ":" + toLineOffset;
	}

	public CodePosition until(CodePosition to) {
		if (!(file == to.file))
			throw new AssertionError("From and to positions must be in the same file!");
		return new CodePosition(file, fromLine, fromLineOffset, to.toLine, to.toLineOffset);
	}

	public CodePosition withLength(int characters) {
		return new CodePosition(file, fromLine, fromLineOffset, fromLine, fromLineOffset + characters);
	}

	public CodePosition end(){
		return new CodePosition(file, toLine, toLineOffset, toLine, toLineOffset);
	}
	public String toString() {
		return fromLine == 0 && fromLineOffset == 0 ? file.getFilename() : file.getFilename() + ":" + Integer.toString(fromLine) + ":" + Integer.toString(fromLineOffset);
	}

	public SourceFile getFile() {
		return file;
	}

	public int getFromLine() {
		return fromLine;
	}

	public int getFromLineOffset() {
		return fromLineOffset;
	}

	public int getToLine() {
		return toLine;
	}

	public int getToLineOffset() {
		return toLineOffset;
	}

	public boolean containsFully(CodePosition other) {
		if (!Objects.equals(this.getFilename(), other.getFilename())) {
			return false;
		}

		//This starts later or ends earlier
		if (this.fromLine > other.fromLine || this.toLine < other.toLine) {
			return false;
		}

		//Same start line but other starts earlier
		if (this.fromLine == other.fromLine && this.fromLineOffset > other.fromLineOffset) {
			return false;
		}

		//Same end line, but other ends later
		if (this.toLine == other.toLine && this.toLineOffset < other.toLineOffset) {
			return false;
		}

		return true;
	}

	@Override
	public int compareTo(CodePosition other) {

        if (this.fromLine != other.fromLine) {
            return Integer.compare(this.fromLine, other.fromLine);
        }
        if (this.fromLineOffset != other.fromLineOffset) {
            return Integer.compare(this.fromLineOffset, other.fromLineOffset);
        }
        if (this.toLine != other.toLine) {
            return Integer.compare(this.toLine, other.toLine);
        }
        return Integer.compare(this.toLineOffset, other.toLineOffset);
	}
}
