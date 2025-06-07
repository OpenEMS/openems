package io.openems.edge.bridge.sml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.FlowControl;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPort;
import org.openmuc.jrxtx.StopBits;
import javax.xml.bind.DatatypeConverter;

public class DummySerialPort implements SerialPort {
	private String smlData = "";
	private String portName;
	private int baudRate;
	private DataBits dataBits;
	private Parity parity;
	private StopBits stopBits;
	private FlowControl flowControl;
	private int timeout;

	DummySerialPort(String portName, int baudRate, int timeout, DataBits dataBits, Parity parity, StopBits stopBits,
			FlowControl flowControl, String smlMessage) {
		this.portName = portName;
		this.baudRate = baudRate;
		this.dataBits = dataBits;
		this.parity = parity;
		this.stopBits = stopBits;
		this.flowControl = flowControl;
		this.timeout = timeout;
		this.smlData = smlMessage;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		DataInputStream inputStream = new DataInputStream(
				new ByteArrayInputStream(DatatypeConverter.parseHexBinary(smlData)));
		return inputStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		DataOutputStream outputStream = new DataOutputStream(new ByteArrayOutputStream());
		return outputStream;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getPortName() {
		return this.portName;
	}

	@Override
	public DataBits getDataBits() {
		return this.dataBits;
	}

	@Override
	public void setDataBits(DataBits dataBits) throws IOException {
		this.dataBits = dataBits;

	}

	@Override
	public Parity getParity() {
		return this.parity;
	}

	@Override
	public void setParity(Parity parity) throws IOException {
		this.parity = parity;

	}

	@Override
	public StopBits getStopBits() {
		return this.stopBits;
	}

	@Override
	public void setStopBits(StopBits stopBits) throws IOException {
		this.stopBits = stopBits;

	}

	@Override
	public int getBaudRate() {
		return this.baudRate;
	}

	@Override
	public void setBaudRate(int baudRate) throws IOException {
		this.baudRate = baudRate;

	}

	@Override
	public int getSerialPortTimeout() {
		return this.timeout;
	}

	@Override
	public void setSerialPortTimeout(int serialPortTimeout) throws IOException {
		this.timeout = serialPortTimeout;

	}

	@Override
	public void setFlowControl(FlowControl flowControl) throws IOException {
		this.flowControl = flowControl;

	}

	@Override
	public FlowControl getFlowControl() {
		return this.flowControl;
	}
	
	public void setSmlMessageString(String smlMessageString) {
		this.smlData = smlMessageString;
	}

}
