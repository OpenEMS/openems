package io.openems.edge.battery.fenecon.home.update.mock;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Set;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.AbstractModbusTransport;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;

public class SerialConnectionMock extends AbstractSerialConnection {
	/**
	 * Creates a serial connection for slave and master.
	 * 
	 * @param parameters Parameters for the serial connection
	 * @return The two serial connection instances.
	 * @throws IOException Never thrown
	 */
	public static SerialConnectionCreatedMock create(SerialParameters parameters) throws IOException {
		var serverOutput = new PipedOutputStream();
		var clientInput = new PipedInputStream(serverOutput);

		var clientOutput = new PipedOutputStream();
		var serverInput = new PipedInputStream(clientOutput);

		return new SerialConnectionCreatedMock(new SerialConnectionMock(parameters, serverOutput, serverInput),
				new SerialConnectionMock(parameters, clientOutput, clientInput));
	}

	private final SerialParameters parameters;

	private boolean isOpen;
	private PipedOutputStream output;
	private PipedInputStream input;
	private AbstractModbusTransport transport;

	public SerialConnectionMock(SerialParameters parameters, PipedOutputStream output, PipedInputStream input) {
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

	public static record SerialConnectionCreatedMock(SerialConnectionMock client, SerialConnectionMock server) {
	}
}
