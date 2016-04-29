package de.fenecon.femscore.modbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.io.ModbusTransaction;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.util.SerialParameters;

public class ModbusRtuConnection extends ModbusConnection {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(ModbusRtuConnection.class);

	private final String serialinterface;
	private final String baudrate;
	private final int databits;
	private final String parity;
	private final int stopbits;
	private SerialConnection con = null;

	public ModbusRtuConnection(String serialinterface, String baudrate, int databits, String parity, int stopbits,
			int cycle) {
		super(cycle);
		this.serialinterface = serialinterface;
		this.baudrate = baudrate;
		this.databits = databits;
		this.parity = parity;
		this.stopbits = stopbits;
	}

	@Override
	public void dispose() {
		if (con != null && con.isOpen()) {
			con.close();
		}
	}

	@Override
	protected ModbusTransaction getTransaction() throws Exception {
		if (con == null) {
			SerialParameters params = new SerialParameters();
			params.setPortName(this.serialinterface);
			params.setBaudRate(this.baudrate);
			params.setDatabits(this.databits);
			params.setParity(this.parity);
			params.setStopbits(this.stopbits);
			params.setEncoding(net.wimpi.modbus.Modbus.SERIAL_ENCODING_RTU);
			params.setEcho(false);
			con = new SerialConnection(params);
		}
		if (!con.isOpen()) {
			con.open();
		}
		ModbusSerialTransaction trans = new ModbusSerialTransaction(con);
		return trans;
	}

	@Override
	public void close() {
		if (con == null) {
			if (con.isOpen()) {
				con.close();
			}
		}
	}

	@Override
	public String toString() {
		return "ModbusRtuConnection [serialinterface=" + serialinterface + ", baudrate=" + baudrate + ", databits="
				+ databits + ", parity=" + parity + ", stopbits=" + stopbits + "]";
	}
}
