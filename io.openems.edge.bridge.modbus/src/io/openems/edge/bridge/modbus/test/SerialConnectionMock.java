package io.openems.edge.bridge.modbus.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.AbstractModbusTransport;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import io.openems.common.function.Disposable;

public class SerialConnectionMock extends AbstractSerialConnection {
	/**
	 * Creates a serial connection for slave and master.
	 *
	 * @param parameters Parameters for the serial connection
	 * @return The two serial connection instances.
	 * @throws IOException Never thrown
	 */
	public static SerialConnectionMockManager create(SerialParameters parameters) throws IOException {
		return new SerialConnectionMockManager(parameters);
	}

	private final SerialParameters parameters;

	private boolean isOpen;
	private final OutputStream output;
	private final InputStream input;
	private AbstractModbusTransport transport;

	protected SerialConnectionMock(SerialParameters parameters, OutputStream output, InputStream input) {
		this.parameters = parameters;
		this.output = output;
		this.input = input;
	}

	@Override
	public void open() {
		if (!this.isOpen) {
			this.isOpen = true;
		}
	}

	@Override
	public AbstractModbusTransport getModbusTransport() {
		return this.transport;
	}

	public void setModbusTransport(AbstractModbusTransport transport) {
		this.transport = transport;
	}

	@Override
	public int readBytes(byte[] buffer, int bytesToRead) {
		this.throwIfConnectionClosed();

		try {
			return this.input.read(buffer, 0, bytesToRead);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public int writeBytes(byte[] buffer, int bytesToWrite) {
		this.throwIfConnectionClosed();

		try {
			this.output.write(buffer, 0, bytesToWrite);
			this.output.flush();
			return bytesToWrite;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public int bytesAvailable() {
		try {
			return this.input.available();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void close() {
		if (!this.isOpen) {
			return;
		}

		try {
			this.isOpen = false;
			this.output.close();
			this.input.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public int getBaudRate() {
		return this.parameters.getBaudRate();
	}

	@Override
	public int getNumDataBits() {
		return this.parameters.getDatabits();
	}

	@Override
	public int getNumStopBits() {
		return this.parameters.getStopbits();
	}

	@Override
	public int getParity() {
		return this.parameters.getParity();
	}

	@Override
	public String getPortName() {
		return this.parameters.getPortName();
	}

	@Override
	public String getDescriptivePortName() {
		return "MOCK";
	}

	@Override
	public void setComPortTimeouts(int i, int i1, int i2) {
	}

	@Override
	public boolean isOpen() {
		return this.isOpen;
	}

	@Override
	public int getTimeout() {
		return Modbus.DEFAULT_TIMEOUT;
	}

	@Override
	public void setTimeout(int i) {
	}

	@Override
	public Set<String> getCommPorts() {
		return Set.of(this.getDescriptivePortName());
	}

	private void throwIfConnectionClosed() {
		if (!this.isOpen) {
			throw new RuntimeException("Connection is closed.");
		}
	}

	public static class SerialConnectionMockManager implements Disposable {
		private final SerialParameters parameters;
		private final SerialConnectionMock server;
		private final ForwardingOutputStream dataFromServer;
		private final PipedOutputStream dataToServer;

		private SerialConnectionMockManager(SerialParameters parameters) throws IOException {
			this.parameters = parameters;
			this.dataFromServer = new ForwardingOutputStream();
			this.dataToServer = new PipedOutputStream();

			this.server = new SerialConnectionMock(parameters, this.dataFromServer, new PipedInputStream(this.dataToServer));
		}

		public SerialConnectionMock getServer() {
			return this.server;
		}

		/**
		 * Creates a new serial connection for a client.
		 *
		 * @return {@link SerialConnectionMock} instance.
		 * @throws IOException Never thrown
		 */
		public SerialConnectionMock createClientConnection() throws IOException {
			var dataToClient = new PipedOutputStream();
			this.dataFromServer.addTargetStream(dataToClient);

			var dataFromClient = new ForwardingOutputStream();
			dataFromClient.addTargetStream(this.dataToServer);

			return new SerialConnectionMock(this.parameters, dataFromClient, new PipedInputStream(dataToClient));
		}

		@Override
		public void dispose() {
			this.server.close();
		}
	}

	private static class ForwardingOutputStream extends OutputStream {
		private final List<OutputStream> targetStreams = new ArrayList<>();

		public void addTargetStream(OutputStream stream) {
			this.targetStreams.add(stream);
		}

		@Override
		public void write(int b) throws IOException {
			for (OutputStream targetStream : this.targetStreams) {
				targetStream.write(b);
			}
		}

		@Override
		public void write(byte[] b) throws IOException {
			for (OutputStream targetStream : this.targetStreams) {
				targetStream.write(b);
			}
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			for (OutputStream targetStream : this.targetStreams) {
				targetStream.write(b, off, len);
			}
		}

		@Override
		public void flush() throws IOException {
			for (OutputStream targetStream : this.targetStreams) {
				targetStream.flush();
			}
		}
	}
}
