package io.openems.edge.bridge.modbus.test;

import com.ghgande.j2mod.modbus.ModbusIOException;
import com.ghgande.j2mod.modbus.io.AbstractModbusTransport;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.AbstractModbusListener;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.net.ModbusSerialListener;

public abstract class ModbusSerialListenerMock extends ModbusSerialListener {

	protected ModbusSerialListenerMock(AbstractSerialConnection serialCon) {
		super(serialCon);
	}

	@Override
	protected void handleRequest(AbstractModbusTransport transport, AbstractModbusListener listener)
			throws ModbusIOException {
		if (transport == null) {
			throw new ModbusIOException("No transport specified");
		}

		final ModbusRequest request = transport.readRequest(listener);
		if (request == null) {
			throw new ModbusIOException("Request for transport %s is invalid (null)",
					transport.getClass().getSimpleName());
		}

		ModbusResponse response;
		try {
			response = this.handle(request);
		} catch (Exception ex) {
			throw new RuntimeException(
					"Exception while handling packet %s".formatted(request.getClass().getSimpleName()), ex);
		}

		if (response != null) {
			response.setTransactionID(request.getTransactionID());
			response.setUnitID(request.getUnitID());
			transport.writeResponse(response);
		}
	}

	protected abstract ModbusResponse handle(ModbusRequest request) throws Exception;
}
