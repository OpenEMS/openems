package io.openems.edge.battery.fenecon.home.update;

import java.io.IOException;

import org.slf4j.Logger;

import com.fazecast.jSerialComm.SerialPort;
import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.AbstractSerialTransportListener;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusMessage;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;

import io.openems.edge.battery.fenecon.home.update.j2mod.BatteryUpdateModbusRtuTransport;

public class SerialPortHandler implements AutoCloseable {

	private final Logger log;
	private final int modbusUnitId;
	protected final AbstractSerialConnection connection;
	protected BatteryUpdateModbusRtuTransport transport;

	public SerialPortHandler(String portName, int baudRate, int modbusUnitId, Logger logger) throws Exception {
		this.log = logger;
		this.modbusUnitId = modbusUnitId;
		this.connection = this.createConnection(portName, baudRate);
		this.initialize();
	}

	protected SerialPortHandler(AbstractSerialConnection connection, int modbusUnitId, Logger logger) throws Exception {
		this.log = logger;
		this.modbusUnitId = modbusUnitId;
		this.connection = connection;
		this.initialize();
	}

	protected AbstractSerialConnection createConnection(String portName, int baudRate) {
		var params = this.buildParameters(portName, baudRate);
		return new SerialConnection(params);
	}

	protected void initialize() throws Exception {
		try {
			this.connection.open();
			this.transport = this.createTransport();
		} catch (Exception ex) {
			throw new Exception("Not able to open port with settings [Port=%s, Baud=%d]"
					.formatted(this.connection.getPortName(), this.connection.getBaudRate()), ex);
		}
	}

	protected BatteryUpdateModbusRtuTransport createTransport() throws IOException {
		var transport = new BatteryUpdateModbusRtuTransport();
		transport.setCommPort(this.connection);
		transport.setEcho(false);

		// Sometimes read after write happens too quickly and causes read errors.
		// Add 5ms additional waiting time between write request and read response
		transport.addListener(new AbstractSerialTransportListener() {
			public void afterMessageWrite(AbstractSerialConnection port, ModbusMessage msg) {
				try {
					SerialPortHandler.this.log.info("Sent " + msg);
					Thread.sleep(5);
				} catch (InterruptedException e) {
					// Empty
				}
			}

			@Override
			public void afterResponseRead(AbstractSerialConnection port, ModbusResponse req) {
				SerialPortHandler.this.log.info("Received " + req);
				super.afterResponseRead(port, req);
			}
		});

		return transport;
	}

	protected SerialParameters buildParameters(String portName, int baudRate) {
		var params = new SerialParameters();
		params.setPortName(portName);
		params.setBaudRate(baudRate);
		params.setOpenDelay(1000);
		params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
		params.setDatabits(8);
		params.setParity(SerialPort.NO_PARITY);
		params.setStopbits(SerialPort.ONE_STOP_BIT);
		params.setFlowControlIn(SerialPort.FLOW_CONTROL_DISABLED);
		params.setEcho(false);
		params.disableRs485Control();

		return params;
	}

	protected ModbusTransaction createTransaction() {
		var transaction = this.transport.createTransaction();
		transaction.setRetries(10);

		return transaction;
	}

	/**
	 * Sends a modbus request to the device, wait for the response and return it.
	 *
	 * @param request Modbus request to send
	 * @return Response from device
	 * @throws ModbusException Thrown in case of modbus errors
	 */
	public ModbusResponse sendRequestAndGetResponse(ModbusRequest request) throws ModbusException {
		request.setUnitID(this.modbusUnitId);

		var transaction = this.createTransaction();
		transaction.setRequest(request);

		transaction.execute();
		return transaction.getResponse();
	}

	/**
	 * Sends a modbus request to the device, wait the given milliseconds, reads the
	 * response and return it. This method can be used if the device needs some time
	 * to process the request.
	 *
	 * @param request    Modbus request to send
	 * @param waitMillis how much milliseconds to wait before reading the response
	 * @return Response from device
	 * @throws ModbusException Thrown in case of modbus errors
	 */
	public ModbusResponse sendRequestAndWaitAndGetResponse(ModbusRequest request, final long waitMillis)
			throws ModbusException {
		var listener = new AbstractSerialTransportListener() {
			@Override
			public void afterMessageWrite(AbstractSerialConnection port, ModbusMessage msg) {
				super.afterMessageWrite(port, msg);
				try {
					Thread.sleep(waitMillis);
				} catch (InterruptedException e) {
					// Empty
				}
			}
		};

		this.transport.addListener(listener);
		try {
			return this.sendRequestAndGetResponse(request);
		} finally {
			this.transport.removeListener(listener);
		}
	}

	@Override
	public void close() throws Exception {
		if (this.connection != null) {
			this.connection.close();
		}
	}
}
