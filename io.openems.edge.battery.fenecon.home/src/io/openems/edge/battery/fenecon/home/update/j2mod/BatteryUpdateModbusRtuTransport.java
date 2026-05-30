package io.openems.edge.battery.fenecon.home.update.j2mod;

import java.io.IOException;

import com.ghgande.j2mod.modbus.io.BytesOutputStream;
import com.ghgande.j2mod.modbus.io.ModbusRTUTransport;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;

public class BatteryUpdateModbusRtuTransport extends ModbusRTUTransport {

	@Override
	protected ModbusRequest createModbusRequest(int functionCode) {
		return switch (functionCode) {
		case ModbusConsts.FUNCTION_CODE_40 -> new FC40Request();
		case ModbusConsts.FUNCTION_CODE_41 -> new FC41Request();
		case ModbusConsts.FUNCTION_CODE_42 -> new FC42Request();
		case ModbusConsts.FUNCTION_CODE_43 -> new FC43Request();
		case ModbusConsts.FUNCTION_CODE_44 -> new FC44Request();
		default -> super.createModbusRequest(functionCode);
		};
	}

	@Override
	protected ModbusResponse createModbusResponse(int functionCode) {
		return switch (functionCode) {
		case ModbusConsts.FUNCTION_CODE_40 -> new FC40Response();
		case ModbusConsts.FUNCTION_CODE_41 -> new FC41Response();
		case ModbusConsts.FUNCTION_CODE_42 -> new FC42Response();
		case ModbusConsts.FUNCTION_CODE_43 -> new FC43Response();
		case ModbusConsts.FUNCTION_CODE_44 -> new FC44Response();
		default -> super.createModbusResponse(functionCode);
		};
	}

	@Override
	protected void getRequest(int function, BytesOutputStream out) throws IOException {
		if ((function & 0x80) == 0) {
			switch (function) {
			case ModbusConsts.FUNCTION_CODE_40:
				readRequestData(5, out);
				return;
			case ModbusConsts.FUNCTION_CODE_41:
			case ModbusConsts.FUNCTION_CODE_43:
			case ModbusConsts.FUNCTION_CODE_44:
				readRequestData(1, out);
				return;
			case ModbusConsts.FUNCTION_CODE_42:
				int amountOfBytes = readByte();
				out.writeByte(amountOfBytes);
				readRequestData(amountOfBytes, out);
				return;
			}
		}

		super.getRequest(function, out);
	}

	@Override
	protected void getResponse(int function, BytesOutputStream out) throws IOException {
		if ((function & 0x80) == 0) {
			switch (function) {
			case ModbusConsts.FUNCTION_CODE_40:
			case ModbusConsts.FUNCTION_CODE_41:
			case ModbusConsts.FUNCTION_CODE_42:
			case ModbusConsts.FUNCTION_CODE_43:
				readRequestData(2, out);
				return;
			case ModbusConsts.FUNCTION_CODE_44:
				readRequestData(3, out);
				return;
			}
		}

		super.getResponse(function, out);
	}

	@Override
	protected int readUid() throws IOException {
		var uid = readByte();
		if (uid == 0) {
			// Somehow the battery sends 0x00 byte at the beginning of F42 responses - we
			// need to skip that byte
			uid = readByte();
		}

		return uid;
	}
}
