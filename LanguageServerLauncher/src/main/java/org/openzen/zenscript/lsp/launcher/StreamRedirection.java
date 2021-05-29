package org.openzen.zenscript.lsp.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamRedirection extends Thread implements AutoCloseable {

	private final InputStream from;
	private final OutputStream to;

	public StreamRedirection(InputStream from, OutputStream to) {
		this.from = from;
		this.to = to;
	}

	@Override
	public void close() throws IOException {
		from.close();
		to.close();
	}

	@Override
	public void run() {
		try {
			int c;
			while((c = from.read()) != -1) {
				to.write(c);
				to.flush();
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}