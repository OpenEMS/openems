package io.openems.edge.bridge.modbus.api.task;

import java.util.BitSet;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.AbstractModbusBridge;

public class Utils {

	public static Boolean[] toBooleanArray(byte[] bytes) {
		BitSet bits = BitSet.valueOf(bytes);
		Boolean[] bools = new Boolean[bytes.length * 8];
		for (int i = bits.nextSetBit(0); i != -1; i = bits.nextSetBit(i + 1)) {
			bools[i] = true;
		}
		return bools;
	}

	public static ModbusResponse getResponse(ModbusRequest request, int unitId, AbstractModbusBridge bridge) throws OpenemsException, ModbusException {
		request.setUnitID(unitId);		
		ModbusTransaction transaction = bridge.getNewModbusTransaction();
		transaction.setRequest(request);
		transaction.execute();
		ModbusResponse response =  transaction.getResponse();
		return response;
	}
}
