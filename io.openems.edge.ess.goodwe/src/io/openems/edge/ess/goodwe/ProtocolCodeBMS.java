package io.openems.edge.ess.goodwe;

import io.openems.common.types.OptionsEnum;

enum ProtocolCodeBMS implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ATS_DEVICE(0x00, "ATSDevice"), //
	PYLONBMS_I(0x03, "PYLONBMS_I"), //
	PYLONBMS_II(0x04, "PYLONBMS_II"), //
	LISHEN_BMS(0x06, "LishenBMS"), //
	BYD_BMS(0x07, "BYDBMS"), //
	LGBMS(0x08, "LGBMS"), //
	BYDBMU(0x09, "BYDBMU"), //
	GCLBMS(0x0B, "GCLBMS"), //
	PYLONBMS_PLUS(0x0C, "PYLONBMS_Plus"), //
	LISHEN_JP(0x0D, "LishenJP"), //
	GOODWE_PROTOCOL(0x1E, "GoodWeProtocol"), //
	PROTO_WARE_HOUSE(0x1F, "ProtoWareHouse"), //
	LEAD_ACID_BATTERY(0x20, "LeadAcidBattery");

	private int value;
	private String option;

	private ProtocolCodeBMS(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return option;
	}
	
	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}	
}