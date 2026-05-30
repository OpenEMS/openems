package io.openems.edge.battery.fenecon.home.update.j2mod;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

abstract class FcCodeResponseBase extends CustomResponse {
	private int statusCode;

	public static final int STATUS_CODE_NOT_READ_YET = -1;

	public FcCodeResponseBase(int functionCode, int statusCode) {
		setFunctionCode(functionCode);
		setDataLength(2);

		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return this.statusCode;
	}

	public String getStatusCodeName() {
		return ResponseStatusCode.byStatusCode(this.statusCode) //
				.map(ResponseStatusCode::name) //
				.orElse(String.valueOf(this.statusCode));
	}

	public boolean isOK() {
		return this.getStatusCode() == ResponseStatusCode.OK.getStatusCode();
	}

	@Override
	public void writeData(DataOutput output) throws IOException {
		output.writeByte(1); // Length of message. Always 1.
		output.writeByte(this.statusCode);
	}

	@Override
	public void readData(DataInput input) throws IOException {
		Utils.readStaticByte(input, (byte) 1, "Invalid data length value");
		this.statusCode = input.readByte();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{StatusCode=%s}".formatted(this.getStatusCodeName());
	}
}
