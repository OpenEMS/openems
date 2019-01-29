package io.openems.edge.battery.soltaro.versionb;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum ErrorCode implements OptionsEnum {
	UNDEFINED(-1, "UNDEFINED", null),
	
	CELL_VOLTAGE_HIGH(0, "Cell voltage high", VersionBChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH),
	TOTAL_VOLTAGE_HIGH(0, "Total voltage high", VersionBChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH),
	CHARGE_CURRENT_HIGH(2, "Charge current high", VersionBChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH),
	CELL_VOLTAGE_LOW(3, "Cell voltage low", VersionBChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW),
	TOTAL_VOLTAGE_LOW(4, "Total voltage low", VersionBChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW),
	DISCHARGE_CURRENT_HIGH(5, "Discharge current high", VersionBChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH),
	CELL_CHARGE_TEMPERATURE_HIGH(6, "Cell charge temperature high", VersionBChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH),
	CELL_CHARGE_TEMPERATURE_LOW(7, "Cell charge temperature low", VersionBChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW),
	SOC_LOW(8, "SoC low", VersionBChannelId.ALARM_LEVEL_2_SOC_LOW),
	TEMPERATURE_DIFFERENCE_HIGH(9, "Temperature difference high", VersionBChannelId.ALARM_LEVEL_2_TEMPERATURE_DIFFERENCE_HIGH),
	POLES_TEMPERATURE_DIFFERENCE_HIGH(10, "Poles temperature difference high", VersionBChannelId.ALARM_LEVEL_2_POLES_TEMPERATURE_DIFFERENCE_HIGH),
	CELL_VOLTAGE_DIFFERENCE_HIGH(11, "Cell voltage difference high", VersionBChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_DIFFERENCE_HIGH),
	INSULATION_LOW(12, "Insulation low", VersionBChannelId.ALARM_LEVEL_2_INSULATION_LOW),
	TOTAL_VOLTAGE_DIFFERENCE_HIGH(13, "Total voltage difference high", VersionBChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_DIFFERENCE_HIGH),
	CELL_DISCHARGE_TEMPERATURE_HIGH(14, "Cell discharge temperature high", VersionBChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH),
	CELL_DISCHARGE_TEMPERATURE_LOW(15, "Cell discharge temperature low", VersionBChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW),
	
	SAMPLING_WIRE(16, "Sampling wire", VersionBChannelId.FAILURE_SAMPLING_WIRE),
	CONNECTOR_WIRE(17, "Connector wire", VersionBChannelId.FAILURE_CONNECTOR_WIRE),
	LTC6803(18, "LTC 6803", VersionBChannelId.FAILURE_LTC6803),
	VOLTAGE_SAMPLING(19, "Voltage sampling", VersionBChannelId.FAILURE_VOLTAGE_SAMPLING),
	TEMP_SAMPLING(20, "Temperature sampling", VersionBChannelId.FAILURE_TEMP_SAMPLING),
	TEMP_SENSOR(21, "Temperature sensor", VersionBChannelId.FAILURE_TEMP_SENSOR),
	GR_T(22, "Gr T", VersionBChannelId.FAILURE_GR_T),
	PCB(23, "PCB", VersionBChannelId.FAILURE_PCB),
	BALANCING_MODULE(24, "Balancing module", VersionBChannelId.FAILURE_BALANCING_MODULE),
	TEMP_SAMPLING_LINE(25, "Temperature sampling line", VersionBChannelId.FAILURE_TEMP_SAMPLING_LINE),
	INTRANET_COMMUNICATION(26, "Intranet communication", VersionBChannelId.FAILURE_INTRANET_COMMUNICATION),
	EEPROM(27, "EEPROM", VersionBChannelId.FAILURE_EEPROM),
	INITIALIZATION(28, "Initialization", VersionBChannelId.FAILURE_INITIALIZATION),
	
	SLAVE_20(29, "Communication error slave 20", VersionBChannelId.SLAVE_20_COMMUNICATION_ERROR),
	SLAVE_19(30, "Communication error slave 19", VersionBChannelId.SLAVE_19_COMMUNICATION_ERROR),
	SLAVE_18(31, "Communication error slave 18", VersionBChannelId.SLAVE_18_COMMUNICATION_ERROR),
	SLAVE_17(32, "Communication error slave 17", VersionBChannelId.SLAVE_17_COMMUNICATION_ERROR),
	SLAVE_16(33, "Communication error slave 16", VersionBChannelId.SLAVE_16_COMMUNICATION_ERROR),
	SLAVE_15(34, "Communication error slave 15", VersionBChannelId.SLAVE_15_COMMUNICATION_ERROR),
	SLAVE_14(35, "Communication error slave 14", VersionBChannelId.SLAVE_14_COMMUNICATION_ERROR),
	SLAVE_13(36, "Communication error slave 13", VersionBChannelId.SLAVE_13_COMMUNICATION_ERROR),
	SLAVE_12(37, "Communication error slave 12", VersionBChannelId.SLAVE_12_COMMUNICATION_ERROR),
	SLAVE_11(38, "Communication error slave 11", VersionBChannelId.SLAVE_11_COMMUNICATION_ERROR),
	SLAVE_10(39, "Communication error slave 10", VersionBChannelId.SLAVE_10_COMMUNICATION_ERROR),
	SLAVE_9(40, "Communication error slave 9", VersionBChannelId.SLAVE_9_COMMUNICATION_ERROR),
	SLAVE_8(41, "Communication error slave 8", VersionBChannelId.SLAVE_8_COMMUNICATION_ERROR),
	SLAVE_7(42, "Communication error slave 7", VersionBChannelId.SLAVE_7_COMMUNICATION_ERROR),
	SLAVE_6(43, "Communication error slave 6", VersionBChannelId.SLAVE_6_COMMUNICATION_ERROR),
	SLAVE_5(44, "Communication error slave 5", VersionBChannelId.SLAVE_5_COMMUNICATION_ERROR),
	SLAVE_4(45, "Communication error slave 4", VersionBChannelId.SLAVE_4_COMMUNICATION_ERROR),
	SLAVE_3(46, "Communication error slave 3", VersionBChannelId.SLAVE_3_COMMUNICATION_ERROR),
	SLAVE_2(47, "Communication error slave 2", VersionBChannelId.SLAVE_2_COMMUNICATION_ERROR),
	SLAVE_1(48, "Communication error slave 1", VersionBChannelId.SLAVE_1_COMMUNICATION_ERROR),
		;

	private ErrorCode(int value, String name, VersionBChannelId errorChannelId) {
		this.value = value;
		this.name = name;
		this.errorChannelId = errorChannelId;
	}
	
	private int value;
	private String name;
	private VersionBChannelId errorChannelId;
	
	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}
	
	public VersionBChannelId getErrorChannelId() {
		return errorChannelId;
	}
	
	public static ErrorCode getErrorCode(VersionBChannelId id) {
		for (ErrorCode code : ErrorCode.values()) {
			if (id.equals(code.errorChannelId)) {
				return code;
			}
		}
		return null;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

}
