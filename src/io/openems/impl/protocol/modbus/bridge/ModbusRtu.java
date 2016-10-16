package io.openems.impl.protocol.modbus.bridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;

import io.openems.api.device.Device;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.api.thing.IsConfigParameter;
import io.openems.impl.protocol.modbus.device.ModbusDevice;

public class ModbusRtu extends ModbusBridge {
	private volatile Integer baudrate = null;
	private SerialConnection connection = null;
	private volatile Integer databits = null;
	private volatile String parity = null;
	private volatile String serialinterface = null;
	private volatile Integer stopbits = null;

	@Override
	public void dispose() {

	}

	@Override
	public ModbusTransaction getTransaction() throws OpenemsModbusException {
		establishModbusConnection(connection);
		ModbusSerialTransaction trans = new ModbusSerialTransaction(connection);
		trans.setRetries(0);
		return trans;
	}

	@IsConfigParameter("baudrate")
	public void setBaudrate(Integer baudrate) {
		this.baudrate = baudrate;
		triggerInitialize();
	}

	@IsConfigParameter("databits")
	public void setDatabits(Integer databits) {
		this.databits = databits;
	}

	@Override
	public void setDevices(Device... devices) {
		super.setDevices(devices);
		triggerInitialize();
	}

	@IsConfigParameter("parity")
	public void setParity(String parity) {
		this.parity = parity;
	}

	@IsConfigParameter("serialinterface")
	public void setSerialinterface(String serialinterface) {
		this.serialinterface = serialinterface;
		triggerInitialize();
	}

	@IsConfigParameter("stopbits")
	public void setStopbits(Integer stopbits) {
		this.stopbits = stopbits;
	}

	@Override
	public String toString() {
		return "ModbusRtu [baudrate=" + baudrate + ", serialinterface=" + serialinterface + ", devices="
				+ Arrays.toString(devices) + "]";
	}

	@Override
	protected boolean initialize() {
		if (baudrate == null || databits == null || parity == null || serialinterface == null || stopbits == null
				|| devices == null || devices.length == 0) {
			return false;
		}
		/*
		 * Copy and cast devices to local modbusdevices array
		 */
		List<ModbusDevice> modbusdevices = new ArrayList<>();
		for (Device device : devices) {
			if (device instanceof ModbusDevice) {
				modbusdevices.add((ModbusDevice) device);
			}
		}
		this.modbusdevices = modbusdevices.stream().toArray(ModbusDevice[]::new);
		/*
		 * Create a new SerialConnection
		 */
		if (connection != null && connection.isOpen()) {
			connection.close();
		}
		SerialParameters params = new SerialParameters();
		params.setPortName(serialinterface);
		params.setBaudRate(baudrate);
		params.setDatabits(databits);
		params.setParity(parity);
		params.setStopbits(stopbits);
		params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
		params.setEcho(false);
		connection = new SerialConnection(params);
		try {
			connection.open();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void establishModbusConnection(SerialConnection connection) throws OpenemsModbusException {
		if (!connection.isOpen()) {
			try {
				connection.open();
			} catch (Exception e) {
				throw new OpenemsModbusException("Unable to open modbus connection: " + connection);
			}
		}
	}
}
