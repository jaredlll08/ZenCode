package org.openzen.zenscript.scripting;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.openzen.zencode.java.logger.ScriptingEngineLogger;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zencode.shared.CompileException;
import org.openzen.zencode.shared.SourceFile;
import org.openzen.zenscript.OpenFileInfo;
import org.openzen.zenscript.validator.ValidationLogEntry;

import java.util.ArrayList;
import java.util.List;

public class DiagnosisLogger implements ScriptingEngineLogger {

	private final List<Diagnostic> caughtDiagnosticEntries = new ArrayList<>();

	@Override
	public void logCompileException(CompileException exception) {
		addDiagnosticEntry(
				exception.position,
				exception.getMessage(),
				DiagnosticSeverity.Error,
				exception.getCode().toString()
		);
	}

	@Override
	public void info(String message) {
	}

	@Override
	public void debug(String message) {

	}

	@Override
	public void trace(String message) {

	}

	@Override
	public void warning(String message) {

	}

	@Override
	public void error(String message) {

	}

	@Override
	public void throwingErr(String message, Throwable throwable) {

	}

	@Override
	public void throwingWarn(String message, Throwable throwable) {

	}

	@Override
	public void logSourceFile(SourceFile file) {
	}

	@Override
	public void logValidationError(ValidationLogEntry errorEntry) {
		addDiagnosticEntry(
				errorEntry.position,
				errorEntry.error.toString(), // TODO
				DiagnosticSeverity.Error,
				errorEntry.error.code.toString()
		);
	}

	private void addDiagnosticEntry(
			CodePosition position,
			String message,
			DiagnosticSeverity severity,
			String code
	) {
		final Range range = OpenFileInfo.codePositionToRange(position);

		final String filename = position.getFilename();
		this.caughtDiagnosticEntries.add(new Diagnostic(range, message, severity, "ZenCode", code));
	}

	@Override
	public void logValidationWarning(ValidationLogEntry warningEntry) {
		addDiagnosticEntry(
				warningEntry.position,
				warningEntry.error.description,
				DiagnosticSeverity.Warning,
				warningEntry.error.code.toString()
		);
	}

	public PublishDiagnosticsParams mergeDiagnosticParams(PublishDiagnosticsParams diagnosticsParams) {
		final ArrayList<Diagnostic> result = new ArrayList<>(diagnosticsParams.getDiagnostics());
		result.addAll(this.caughtDiagnosticEntries);

		final String uri = diagnosticsParams.getUri();
		return new PublishDiagnosticsParams(uri, result);
	}

	public PublishDiagnosticsParams mergeDiagnosticParams(String uri, List<Diagnostic> initial) {
		final ArrayList<Diagnostic> result = new ArrayList<>(initial);
		result.addAll(this.caughtDiagnosticEntries);

		return new PublishDiagnosticsParams(uri, result);
	}

}
