package io.openems.edge.meter.entes.mpr15s22;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface Mpr15S22 extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		VOLTAGE_L1_N(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		VOLTAGE_L2_N(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		VOLTAGE_L3_N(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		VOLTAGE_L4_N(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		VOLTAGE_L1_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		VOLTAGE_L2_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		VOLTAGE_L3_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
//		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
//				.unit(Unit.AMPERE)), //
//		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
//				.unit(Unit.AMPERE)), //
//		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
//				.unit(Unit.AMPERE)), //
		CURRENT_L4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		NEUTRAL_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		MEASURED_FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //
		ACTIVE_POWER_L1_N(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT)), //
		ACTIVE_POWER_L2_N(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT)), //
		ACTIVE_POWER_L3_N(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT)), //
		ACTIVE_POWER_L4_N(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT)), //
		TOTAL_IMPORT_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT)), //
		TOTAL_EXPORT_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT)), //
		SUM_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT)), //
//		REACTIVE_POWER_L1(Doc.of(OpenemsType.FLOAT) //
//				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
//		REACTIVE_POWER_L2(Doc.of(OpenemsType.FLOAT) //
//				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
//		REACTIVE_POWER_L3(Doc.of(OpenemsType.FLOAT) //
//				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		REACTIVE_POWER_L4(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		QUADRANT_1_TOTAL_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		QUADRANT_2_TOTAL_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		QUADRANT_3_TOTAL_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		QUADRANT_4_TOTAL_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		SUM_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		APPARENT_POWER_L1_N(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER_L2_N(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER_L3_N(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER_L4_N(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		TOTAL_IMPORT_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		TOTAL_EXPORT_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		SUM_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		POWER_FACTOR_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		POWER_FACTOR_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		POWER_FACTOR_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		POWER_FACTOR_L4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		SUM_POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		COSPHI_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		COSPHI_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		COSPHI_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		COSPHI_L4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		SUM_COS_PHI(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		ROTATION_FIELD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		VOLTAGE_UNBALANCE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)), //
		CURRENT_UNBALANCE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)), //
		L1_PHASE_VOLTAGE_ANGLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		L2_PHASE_VOLTAGE_ANGLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		L3_PHASE_VOLTAGE_ANGLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		L4_PHASE_VOLTAGE_ANGLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		L1_PHASE_CURRENT_ANGLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		L2_PHASE_CURRENT_ANGLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		L3_PHASE_CURRENT_ANGLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		L4_PHASE_CURRENT_ANGLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		ANALOG_INPUT_1(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		ANALOG_INPUT_2(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		ANALOG_INPUT_3(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		ANALOG_INPUT_4(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		ANALOG_INPUT_5(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		ANALOG_INPUT_6(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		ANALOG_INPUT_7(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		ANALOG_INPUT_8(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		ANALOG_OUTPUT_1(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		ANALOG_OUTPUT_2(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		ANALOG_OUTPUT_3(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		ANALOG_OUTPUT_4(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		TEMPERATURE_INPUT_1(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_INPUT_2(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_INPUT_3(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_INPUT_4(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_INPUT_5(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		TEMPERATURE_INPUT_6(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		TEMPERATURE_INPUT_7(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		TEMPERATURE_INPUT_8(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		HOUR_METER_NON_RESETABLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HOUR)), //
		WORKING_HOUR_COUNTER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HOUR)), //
		INPUT_STATUS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		OUTPUT_STATUS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		CONSUMED_ACTIVE_ENERGY_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		CONSUMED_ACTIVE_ENERGY_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		CONSUMED_ACTIVE_ENERGY_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		CONSUMED_ACTIVE_ENERGY_L4(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		TOTAL_CONSUMED_ENERGY_L1_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		DELIVERED_ACTIVE_ENERGY_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		DELIVERED_ACTIVE_ENERGY_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		DELIVERED_ACTIVE_ENERGY_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		DELIVERED_ACTIVE_ENERGY_L4(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		TOTAL_DELIVERED_ENERGY_L1_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		CONSUMED_APPARENT_ENERGY_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_HOURS)), //
		CONSUMED_APPARENT_ENERGY_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_HOURS)), //
		CONSUMED_APPARENT_ENERGY_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_HOURS)), //
		CONSUMED_APPARENT_ENERGY_L4(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_HOURS)), //
		TOTAL_CONSUMED_APPARENT_ENERGY_L1_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_HOURS)), //
		DELIVERED_APPARENT_ENERGY_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_HOURS)), //
		DELIVERED_APPARENT_ENERGY_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_HOURS)), //
		DELIVERED_APPARENT_ENERGY_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_HOURS)), //
		DELIVERED_APPARENT_ENERGY_L4(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_HOURS)), //
		TOTAL_DELIVERED_APPARENT_ENERGY_L1_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_HOURS)), //
		QUADRANT_1_REACTIVE_ENERGY_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_1_REACTIVE_ENERGY_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_1_REACTIVE_ENERGY_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_1_REACTIVE_ENERGY_L4(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_1_TOTAL_REACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_2_REACTIVE_ENERGY_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_2_REACTIVE_ENERGY_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_2_REACTIVE_ENERGY_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_2_REACTIVE_ENERGY_L4(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_2_TOTAL_REACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_3_REACTIVE_ENERGY_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_3_REACTIVE_ENERGY_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_3_REACTIVE_ENERGY_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_3_REACTIVE_ENERGY_L4(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_3_TOTAL_REACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_4_REACTIVE_ENERGY_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_4_REACTIVE_ENERGY_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_4_REACTIVE_ENERGY_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_4_REACTIVE_ENERGY_L4(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		QUADRANT_4_TOTAL_REACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		NUMBER_OF_PULSE_METER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		TOTAL_PULSE_METER_INPUT_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		TOTAL_PULSE_METER_INPUT_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		TOTAL_PULSE_METER_INPUT_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		TOTAL_PULSE_METER_INPUT_4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		TOTAL_PULSE_METER_INPUT_5(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		TOTAL_PULSE_METER_INPUT_6(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		TOTAL_PULSE_METER_INPUT_7(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		TOTAL_PULSE_METER_INPUT_8(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE));

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
