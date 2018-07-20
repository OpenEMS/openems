package io.openems.edge.bridge.modbus.api.task;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.util.BitVector;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.AbstractModbusBridge;

public class Utils {

	public static Boolean[] toBooleanArray(BitVector v) {
		Boolean[] bools = new Boolean[v.size()];
		for (int i = 0; i < v.size(); i++) {
			bools[i] = v.getBit(i);
		}
		return bools;
	}
	
	public static Boolean[] toBooleanArray(byte[] bytes) {
		Boolean[] bools = new Boolean[bytes.length * 8];
		for (int i = 0; i < bytes.length * 8; i++) {
			int byteIndex = i / 8;
			bools[i] = (bytes[byteIndex] & (byte) (128 / Math.pow(2, i % 8))) != 0;
		}
		return bools;
	}

	public static ModbusResponse getResponse(ModbusRequest request, int unitId, AbstractModbusBridge bridge)
			throws OpenemsException, ModbusException {
		request.setUnitID(unitId);
		ModbusTransaction transaction = bridge.getNewModbusTransaction();
		transaction.setRequest(request);
		transaction.execute();
		ModbusResponse response = transaction.getResponse();
		return response;
	}

	public static String toBitString(InputRegister[] registers) {
		return Arrays.stream(registers).map(register -> {
			byte[] bs = register.toBytes();

			return String.format("%8s", //
					Integer.toBinaryString(bs[0] & 0xFF)).replace(' ', '0') //
					+ " " //
					+ String.format("%8s", //
							Integer.toBinaryString(bs[1] & 0xFF)).replace(' ', '0');
		}).collect(Collectors.joining(" "));
	}

	public static String toBitString(byte[] bs) {
		return IntStream.range(0, bs.length).map(idx -> bs[idx]).mapToObj(b -> {
			return String.format("%8s", //
					Integer.toBinaryString((byte) b & 0xFF)).replace(' ', '0');
		}).collect(Collectors.joining(" "));
	}
}
