package io.openems.edge.battery.fenecon.home.update.j2mod;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ghgande.j2mod.modbus.msg.ModbusResponse;

public abstract class CustomResponse extends ModbusResponse {
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
}
