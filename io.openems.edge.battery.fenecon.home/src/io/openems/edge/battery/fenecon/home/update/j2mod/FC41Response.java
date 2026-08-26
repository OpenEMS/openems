package io.openems.edge.battery.fenecon.home.update.j2mod;

public class FC41Response extends FcCodeResponseBase {
	public FC41Response() {
		this(STATUS_CODE_NOT_READ_YET);
	}

	public FC41Response(int statusCode) {
		super(ModbusConsts.FUNCTION_CODE_41, statusCode);
	}
}
