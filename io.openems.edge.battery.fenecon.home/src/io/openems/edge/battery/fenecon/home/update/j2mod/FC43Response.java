package io.openems.edge.battery.fenecon.home.update.j2mod;

public class FC43Response extends FcCodeResponseBase {
	public FC43Response() {
		this(STATUS_CODE_NOT_READ_YET);
	}

	public FC43Response(int statusCode) {
		super(ModbusConsts.FUNCTION_CODE_43, statusCode);
	}
}
