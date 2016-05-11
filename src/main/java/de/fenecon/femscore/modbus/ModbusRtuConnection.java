package de.fenecon.femscore.modbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;

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
			con = null;
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
			params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
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
