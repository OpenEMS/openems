package io.openems.edge.battery.fenecon.home.update.j2mod;

import static io.openems.edge.battery.fenecon.home.update.j2mod.Utils.updateResponseWithHeader;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.ghgande.j2mod.modbus.msg.ModbusResponse;

public class FC40Request extends CustomRequest {
	private long fileSize;

	public FC40Request() {
		this(0);
	}

	public FC40Request(long fileSize) {
		super();

		setFunctionCode(ModbusConsts.FUNCTION_CODE_40);
		setDataLength(5);

		this.fileSize = fileSize;
	}

	public long getFileSize() {
		return this.fileSize;
	}

	@Override
	public ModbusResponse getResponse() {
		return updateResponseWithHeader(this, new FC40Response());
	}

	@Override
	public void writeData(DataOutput dout) throws IOException {
		byte[] data = Utils.uintTo4Bytes(this.fileSize);

		dout.writeByte(data.length);
		dout.write(data);
	}

	@Override
	public void readData(DataInput din) throws IOException {
		Utils.readStaticByte(din, (byte) 4, "Invalid data length value");

		byte[] data = new byte[4];
		din.readFully(data);

		this.fileSize = Utils.bytes4ToUint(data);
	}

	@Override
	public String toString() {
		return "FC41Request{fileSize=%d}".formatted(this.fileSize);
	}
}
