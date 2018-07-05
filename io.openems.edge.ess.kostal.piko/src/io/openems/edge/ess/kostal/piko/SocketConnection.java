package io.openems.edge.ess.kostal.piko;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketConnection {

	private final Logger log = LoggerFactory.getLogger(SocketConnection.class);

	private final String host;
	private final int port;

	private Socket socket = null;
	private OutputStream out = null;
	private InputStream in = null;

	public SocketConnection(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void open() throws IOException {
		if (this.socket != null && this.socket.isConnected()) {
			return;
		}
		Socket socket = new Socket(this.host, this.port);
		this.out = socket.getOutputStream();
		this.in = socket.getInputStream();
		this.socket = socket;
	}

	public void close() {
		if (in != null) {
			try {
				this.in.close();
			} catch (IOException e) {
				this.log.error(e.getMessage());
			}
		}
		if (out != null) {
			try {
				this.out.close();
			} catch (IOException e) {
				this.log.error(e.getMessage());
			}
		}
		if (this.socket != null) {
			try {
				this.socket.close();
			} catch (IOException e) {
				this.log.error(e.getMessage());
			}
		}
	}

	public OutputStream getOut() {
		return out;
	}

	public InputStream getIn() {
		return in;
	}
}
