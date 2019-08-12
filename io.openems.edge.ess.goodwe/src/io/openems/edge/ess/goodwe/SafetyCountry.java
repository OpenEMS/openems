package io.openems.edge.ess.goodwe;

import io.openems.common.types.OptionsEnum;

enum SafetyCountry implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ITALY(0, "Italy"), //
	CZECH(1, "CZECH"), //
	GERMANY(2, "GERMANY"), //
	SPAIN(3, "SPAIN"), //
	GREECE_MAINLAND(4, "GREECE MAINLAND"), //
	DENMARK(5, "DENMARK"), // 
	BELGIUM(6, "BELGIUM"), // 
	ROMANIA(7, "ROMANIA"), //
	G83_G59(8, "G83 or G59, according to model"), //
	AUSTRALIA(9, "Australia"), //
	FRANCE(0x0A, "France"), //
	CHINA(0x0B, "China"), //
	GRID_DEFAULT_60HZ(0x0C, "60Hz Grid Default"), //
	POLAND(0x0D, "Poland"), // 
	SOUTH_AFRICA(0x0E, "South Africa"), // 
	AUSTRALIA_L(0x0F, "AustraliaL"), //
	BRAZIL(0x10, "Brazil"), //
	THAILAND_MEA(0x11, "Thailand MEA"), //
	THAILAND_PEA(0x12, "Thailand PEA"), //
	MAURITIUS(0x13, "Mauritius"), // 
	HOLLAND(0x14, "Holland"), // 
	NORTHERN_IRELAND(0x15, "Northern Ireland"), //
	CHINESE_STANDARD_HIGHER(0x16, "Chinese Standard higher performance"), //
	FRENCH_50HZ(0x17, "French 50Hz"), //
	FRENCH_60HZ(0x18, "French 60Hz"), //
	AUSTRALIA_ERGON(0x19, "Australia Ergon"), //
	AUSTRALIA_ENERGEX(0x1A, "Australia Energex"), //
	HOLLAND_16_20A(0x1B, "Holland 16/20A"), // 
	KOREA(0x1C, "Korea"), // 
	CHINA_STATION(0x1D, "China Station"), //
	AUSTRIA(0x1E, "Austria"), //
	INDIA(0x1F, "India"), //
	GRID_DEFAULT_50HZ(0x20, "50Hz Grid Default"), //
	WAREHOUSE(0x21, "Warehouse"), //
	PHILIPPINES(0x22, "Philippines"), //
	IRELAND(0x23, "Ireland"), //
	TAIWAN(0x24, "Taiwan"), // 
	BULGARIA(0x25, "Bulgaria"), // 
	BARBADOS(0x26, "Barbados"), //
	CHINESE_STANDARD_HIGHEST(0x27, "Chinese Standard highest performance"); //
	
	private int value;
	private String option;

	private SafetyCountry(int value, String option) {
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