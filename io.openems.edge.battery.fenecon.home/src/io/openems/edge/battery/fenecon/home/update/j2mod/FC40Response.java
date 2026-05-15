package io.openems.edge.battery.fenecon.home.update.j2mod;

public class FC40Response extends FcCodeResponseBase {
	public FC40Response() {
		this(STATUS_CODE_NOT_READ_YET);
	}

	public FC40Response(int statusCode) {
		super(ModbusConsts.FUNCTION_CODE_40, statusCode);
	}
}
