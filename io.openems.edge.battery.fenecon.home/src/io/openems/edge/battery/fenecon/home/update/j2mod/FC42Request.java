package io.openems.edge.battery.fenecon.home.update.j2mod;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.ghgande.j2mod.modbus.msg.ModbusResponse;

public class FC42Request extends CustomRequest {
	public static final int MAX_DATA_LEN = 128;

	/**
	 * The data can't be send in a single request, so it's splitted into multiple
	 * requests. This integer specifies the current frame index that is transmitted.
	 * First frame is index 1.
	 */
	private int frameIndex;
	private byte[] data;

	public FC42Request() {
		this(1, new byte[0]);
	}

	public FC42Request(int frameIndex, byte[] data) {
		super();

		this.frameIndex = frameIndex;
		this.data = data;

		setFunctionCode(ModbusConsts.FUNCTION_CODE_42);
		setDataLength(this.data.length + 3);
	}

	@Override
	public ModbusResponse getResponse() {
		return null;
	}

	@Override
	public void writeData(DataOutput dout) throws IOException {
		dout.writeByte(this.data.length + 2);
		dout.writeShort(this.frameIndex);
		dout.write(this.data);
	}

	@Override
	public void readData(DataInput din) throws IOException {
		int amountOfBytes = din.readUnsignedByte();
		this.data = new byte[amountOfBytes - 2];

		this.frameIndex = din.readUnsignedShort();
		din.readFully(this.data);
	}

	public int getFrameIndex() {
		return this.frameIndex;
	}

	public byte[] getData() {
		return this.data;
	}

	@Override
	public String toString() {
		return "FC42Request{FrameIndex=%d}".formatted(this.frameIndex);
	}
}
