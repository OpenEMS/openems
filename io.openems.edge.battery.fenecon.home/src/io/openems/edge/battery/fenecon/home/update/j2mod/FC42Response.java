package io.openems.edge.battery.fenecon.home.update.j2mod;

public class FC42Response extends FcCodeResponseBase {

	public FC42Response() {
		this(STATUS_CODE_NOT_READ_YET);
	}

	public FC42Response(int statusCode) {
		super(ModbusConsts.FUNCTION_CODE_42, statusCode);
	}
}
