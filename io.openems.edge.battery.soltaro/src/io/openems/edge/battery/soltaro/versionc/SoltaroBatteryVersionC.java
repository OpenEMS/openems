package io.openems.edge.battery.soltaro.versionc;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.common.enums.EmsBaudrate;
import io.openems.edge.common.channel.Doc;

public interface SoltaroBatteryVersionC extends Battery {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/*
		 * EnumWriteChannels
		 */
		EMS_BAUDRATE(Doc.of(EmsBaudrate.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		/*
		 * IntegerWriteChannels
		 */
		EMS_ADDRESS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		EMS_COMMUNICATION_TIMEOUT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //

		/*
		 * StateChannels
		 */
		// Other Alarm Info
		ALARM_COMMUNICATION_TO_MASTER_BMS(Doc.of(Level.WARNING) //
				.text("Communication Failure to Master BMS")), //
		ALARM_COMMUNICATION_TO_SLAVE_BMS(Doc.of(Level.WARNING) //
				.text("Communication Failure to Slave BMS")), //
		ALARM_COMMUNICATION_SLAVE_BMS_TO_TEMP_SENSORS(Doc.of(Level.WARNING) //
				.text("Communication Failure between Slave BMS and Temperature Sensors")), //
		ALARM_SLAVE_BMS_HARDWARE(Doc.of(Level.WARNING) //
				.text("Slave BMS Hardware Failure")), //
		// Pre-Alarm
		PRE_ALARM_CELL_VOLTAGE_HIGH(Doc.of(OpenemsType.BOOLEAN) //
				.text("Cell Voltage High Pre-Alarm")), //
		PRE_ALARM_TOTAL_VOLTAGE_HIGH(Doc.of(Level.INFO) //
				.text("Total Voltage High Pre-Alarm")), //
		PRE_ALARM_CHARGE_CURRENT_HIGH(Doc.of(Level.INFO) //
				.text("Charge Current High Pre-Alarm")), //
		PRE_ALARM_CELL_VOLTAGE_LOW(Doc.of(OpenemsType.BOOLEAN) //
				.text("Cell Voltage Low Pre-Alarm")), //
		PRE_ALARM_TOTAL_VOLTAGE_LOW(Doc.of(Level.INFO) //
				.text("Total Voltage Low Pre-Alarm")), //
		PRE_ALARM_DISCHARGE_CURRENT_HIGH(Doc.of(Level.INFO) //
				.text("Discharge Current High Pre-Alarm")), //
		PRE_ALARM_CHARGE_TEMP_HIGH(Doc.of(Level.INFO) //
				.text("Charge Temperature High Pre-Alarm")), //
		PRE_ALARM_CHARGE_TEMP_LOW(Doc.of(Level.INFO) //
				.text("Charge Temperature Low Pre-Alarm")), //
		PRE_ALARM_SOC_LOW(Doc.of(OpenemsType.BOOLEAN) //
				.text("State-Of-Charge Low Pre-Alarm")), //
		PRE_ALARM_TEMP_DIFF_TOO_BIG(Doc.of(Level.INFO) //
				.text("Temperature Difference Too Big Pre-Alarm")), //
		PRE_ALARM_POWER_POLE_HIGH(Doc.of(Level.INFO) //
				.text("Power Pole Temperature High Pre-Alarm")), //
		PRE_ALARM_CELL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.INFO) //
				.text("Cell Voltage Difference Too Big Pre-Alarm")), //
		PRE_ALARM_INSULATION_FAIL(Doc.of(Level.INFO) //
				.text("Insulation Failure Pre-Alarm")), //
		PRE_ALARM_TOTAL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.INFO) //
				.text("Total Voltage Difference Too Big Pre-Alarm")), //
		PRE_ALARM_DISCHARGE_TEMP_HIGH(Doc.of(Level.INFO) //
				.text("Discharge Temperature High Pre-Alarm")), //
		PRE_ALARM_DISCHARGE_TEMP_LOW(Doc.of(Level.INFO) //
				.text("Discharge Temperature Low Pre-Alarm")), //
		// Alarm Level 1
		LEVEL1_DISCHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Discharge Temperature Low Alarm Level 1")), //
		LEVEL1_DISCHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Discharge Temperature High Alarm Level 1")), //
		LEVEL1_TOTAL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Total Voltage Difference Too Big Alarm Level 1")), //
		LEVEL1_INSULATION_VALUE(Doc.of(Level.WARNING) //
				.text("Insulation Value Failure Alarm Level 1")), //
		LEVEL1_CELL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Cell Voltage Difference Too Big Alarm Level 1")), //
		LEVEL1_POWER_POLE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Power Pole temperature too high Alarm Level 1")), //
		LEVEL1_TEMP_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Temperature Difference Too Big Alarm Level 1")), //
		LEVEL1_CHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cell Charge Temperature Low Alarm Level 1")), //
		LEVEL1_SOC_LOW(Doc.of(Level.WARNING) //
				.text("Stage-Of-Charge Low Alarm Level 1")), //
		LEVEL1_CHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Charge Temperature High Alarm Level 1")), //
		LEVEL1_DISCHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Discharge Current High Alarm Level 1")), //
		LEVEL1_TOTAL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Total Voltage Low Alarm Level 1")), //
		LEVEL1_CELL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cell Voltage Low Alarm Level 1")), //
		LEVEL1_CHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Charge Current High Alarm Level 1")), //
		LEVEL1_TOTAL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Total Voltage High Alarm Level 1")), //
		LEVEL1_CELL_VOLTAGE_HIGH(Doc.of(OpenemsType.BOOLEAN) //
				.text("Cell Voltage High Alarm Level 1")), //
		// Alarm Level 2
		LEVEL2_DISCHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Discharge Temperature Low Alarm Level 2")), //
		LEVEL2_DISCHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Discharge Temperature High Alarm Level 2")), //
		LEVEL2_TOTAL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Total Voltage Difference Too Big Alarm Level 2")), //
		LEVEL2_INSULATION_VALUE(Doc.of(Level.WARNING) //
				.text("Insulation Value Failure Alarm Level 2")), //
		LEVEL2_CELL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Cell Voltage Difference Too Big Alarm Level 2")), //
		LEVEL2_POWER_POLE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Power Pole temperature too high Alarm Level 2")), //
		LEVEL2_TEMP_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Temperature Difference Too Big Alarm Level 2")), //
		LEVEL2_CHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cell Charge Temperature Low Alarm Level 2")), //
		LEVEL2_SOC_LOW(Doc.of(Level.WARNING) //
				.text("Stage-Of-Charge Low Alarm Level 2")), //
		LEVEL2_CHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Charge Temperature High Alarm Level 2")), //
		LEVEL2_DISCHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Discharge Current High Alarm Level 2")), //
		LEVEL2_TOTAL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Total Voltage Low Alarm Level 2")), //
		LEVEL2_CELL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cell Voltage Low Alarm Level 2")), //
		LEVEL2_CHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Charge Current High Alarm Level 2")), //
		LEVEL2_TOTAL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Total Voltage High Alarm Level 2")), //
		LEVEL2_CELL_VOLTAGE_HIGH(Doc.of(Level.INFO) //
				.text("Cell Voltage High Alarm Level 2")), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
