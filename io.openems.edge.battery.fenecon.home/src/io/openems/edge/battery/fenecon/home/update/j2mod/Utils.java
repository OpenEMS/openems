package io.openems.edge.battery.fenecon.home.update.j2mod;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;

class Utils {
	/**
	 * Updates the response with the header information to match the request.
	 *
	 * @param request  Request to get data from
	 * @param response Response to update
	 * @return Updated response
	 */
	static ModbusResponse updateResponseWithHeader(ModbusRequest request, ModbusResponse response) {

		// transfer header data
		response.setHeadless(request.isHeadless());
		if (!request.isHeadless()) {
			response.setTransactionID(request.getTransactionID());
			response.setProtocolID(request.getProtocolID());
		} else {
			response.setHeadless();
		}
		response.setUnitID(request.getUnitID());
		return response;
	}

	static byte[] uintTo4Bytes(long unsignedIntNumber) {
		return ByteBuffer.allocate(4).putInt((int) (unsignedIntNumber & 0xFFFFFFFFL)).array();
	}

	static long bytes4ToUint(byte[] bytes) {
		return (long) ByteBuffer.wrap(bytes).getInt() & 0xFFFFFFFFL;
	}

	static void readStaticByte(DataInput din, byte expected, String message) throws IOException {
		byte data = din.readByte();
		if (data != expected) {
			throw new IOException("Expected %d, but got %d: %s".formatted(expected, data, message));
		}
	}
}
