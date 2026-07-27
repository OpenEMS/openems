package io.openems.edge.battery.fenecon.home.update.j2mod;

import static io.openems.edge.battery.fenecon.home.update.j2mod.Utils.updateResponseWithHeader;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.ghgande.j2mod.modbus.msg.ModbusResponse;

public class FC43Request extends CustomRequest {
	public FC43Request() {
		super();

		setFunctionCode(ModbusConsts.FUNCTION_CODE_43);
		setDataLength(1);
	}

	@Override
	public byte[] getMessage() {
		return new byte[] { 0x00 };
	}

	@Override
	public ModbusResponse getResponse() {
		return updateResponseWithHeader(this, new FC43Response());
	}

	@Override
	public void writeData(DataOutput dout) throws IOException {
		dout.writeByte(0x00);
	}

	@Override
	public void readData(DataInput din) throws IOException {
		Utils.readStaticByte(din, (byte) 0, "Length field that should be zero");
	}
}
