package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum SafetyCountry implements OptionsEnum {
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
	G59_3(0x28, "G59/3"), //
	SWEDEN(0x29, "Sweden"), //
	CHILE(0x2A, "Chile"), //
	BRAZIL_LV(0x2B, "Brazil LV"), //
	NEWZEALAND(0x2C, "NewZealand"), //
	IEEE1547_208VAC(0x2D, "IEEE1547 208Vac"), //
	IEEE1547_220VAC(0x2E, "IEEE1547 220Vac"), //
	IEEE1547_240VAC(0x2F, "IEEE1547 240Vac"), //
	DEFAULT_60_HZ_LV(0x30, "60Hz LV Default"), //
	DEFAULT_50_HZ_LV(0x31, "50Hz LV Default"), //

	AUSTRALIA_WESTERN(0x32, "Australia Western"), //
	AUSTRALIA_MICRO_GRID(0x33, "Australia MicroGrid"), //
	JP_50_HZ(0x34, "JP_50Hz"), //
	JP_60_HZ(0x35, "JP_60Hz"), //
	INDIA_HIGHER(0x36, "India Higher"), //
	DEWA_LV(0x37, "DEWA LV"), //
	DEWA_MV(0x38, "DEWA MV"), //
	SLOVAKIA(0x39, "Slovakia"), //
	GREEN_GRID(0x3A, "GreenGrid"), //
	HUNGARY(0x3B, "Hungary"), //
	SRILANKA(0x3C, "SriLanka"), //
	SPAIN_ISLANDS(0x3D, "SpainIslands"), //
	ERAGON_30_K(0x3E, "Ergon30K"), //
	ENERGE_30_K(0x3F, "Energe30K"), //
	IEEE1547_230VAC(0x40, "IEEE1547_230VAC"), //
	IEC61727_60HZ(0x41, "IEC61727_60Hz"), //
	SWITZERLAND(0x42, "Switzerland"), //
	CEI_016(0x43, "CEI_016"), //
	AUSTRALIA_HORIZON(0x44, "Australia Horizon"), //
	CYPRUS(0x45, "Cyprus"), //
	AUSTRALIA_SAPN(0x46, "Australia SAPN"), //
	AUSTRALIA_AUSGRID(0x47, "Australia Ausgrid"), //
	AUSTRALIA_ESSENTIAL(0x48, "Australia Essential"), //
	AUSTRALIA_PWCORE_CITI_PW(0x49, "Australia Pwcore_CitiPW"), //

	// TODO: No Grid connection standards given by GoodWe
	// CHINA_HONGKONG(0x31, "China Hongkong"), //
	// POLAND_MV(0x31, "Poland MV"), //
	// HOLLAND_MV(0x31, "Holland MV"), //
	// SWEDEN_MV(0x31, "Sweden MV"), //
	// VDE4110(0x31, "VDE4110"), //
	// SPAIN_MV(0x31, "Spain MV"), //
	// (0x31, "Australia Endeavour"), //
	// DEFAULT_50_HZ_LV(0x31, "Argentina"), //
	// DEFAULT_50_HZ_LV(0x31, "AustralianB"), //
	// DEFAULT_50_HZ_LV(0x31, "AustralianC")
	;

	private final int value;
	private final String option;

	private SafetyCountry(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}