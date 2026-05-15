package io.openems.edge.battery.fenecon.home.update.j2mod;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class FC44Response extends CustomResponse {
	private int statusCode;
	private int progressPercentage;

	public static final int NUMBER_NOT_READ_YET = -1;

	public FC44Response() {
		this(NUMBER_NOT_READ_YET, NUMBER_NOT_READ_YET);
	}

	public FC44Response(int statusCode, int progressPercentage) {
		super();

		setFunctionCode(ModbusConsts.FUNCTION_CODE_44);
		setDataLength(3);

		this.statusCode = statusCode;
		this.progressPercentage = progressPercentage;
	}

	public int getStatusCode() {
		return this.statusCode;
	}

	public int getProgressPercentage() {
		return this.progressPercentage;
	}

	@Override
	public void writeData(DataOutput output) throws IOException {
		output.writeByte(0x02);
		output.writeByte(this.statusCode);
		output.writeByte(this.progressPercentage);
	}

	@Override
	public void readData(DataInput input) throws IOException {
		Utils.readStaticByte(input, (byte) 2, "Invalid data length value");
		this.statusCode = input.readUnsignedByte();
		this.progressPercentage = input.readUnsignedByte();
	}

	@Override
	public String toString() {
		return "FC44Response{StatusCode=%d,Progress=%d}".formatted(this.statusCode, this.progressPercentage);
	}
}
