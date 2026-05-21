package io.openems.edge.battery.fenecon.home.update.j2mod;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.AbstractModbusListener;

public abstract class CustomRequest extends ModbusRequest {
	@Override
	public ModbusResponse createResponse(AbstractModbusListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] getMessage() {
		var memoryStream = new ByteArrayOutputStream();
		try {
			try (var dataStream = new DataOutputStream(memoryStream)) {
				this.writeData(dataStream);
				return memoryStream.toByteArray();
			}
		} catch (IOException e) {
			throw new RuntimeException("Error while writing data to byte array output stream", e);
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
