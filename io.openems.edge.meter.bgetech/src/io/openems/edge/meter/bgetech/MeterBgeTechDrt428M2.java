package io.openems.edge.meter.bgetech;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface MeterBgeTechDrt428M2 extends ElectricityMeter, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// Apparent Power
		TOTAL_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		L1_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		L2_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		L3_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //

		// Power Factor
		TOTAL_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		L1_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		L2_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		L3_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //

		// Active Energy
		TOTAL_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		L1_TOTAL_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		L2_TOTAL_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		L3_TOTAL_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //

		// Forward Active Energy
		FORWARD_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		L1_FORWARD_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		L2_FORWARD_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		L3_FORWARD_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //

		// Reverse Active Energy
		REVERSE_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		L1_REVERSE_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		L2_REVERSE_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		L3_REVERSE_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //

		// Reactive Energy
		TOTAL_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		L1_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		L2_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		L3_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //

		// Forward Reactive Energy
		FORWARD_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		L1_FORWARD_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		L2_FORWARD_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		L3_FORWARD_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //

		// Reverse Reactive Energy
		REVERSE_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		L1_REVERSE_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		L2_REVERSE_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		L3_REVERSE_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //

		// T1 Active Energy
		T1_TOTAL_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		T1_FORWARD_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		T1_REVERSE_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //

		// T1 Reactive Energy
		T1_TOTAL_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		T1_FORWARD_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		T1_REVERSE_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //

		// T2 Active Energy
		T2_TOTAL_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		T2_FORWARD_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		T2_REVERSE_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //

		// T2 Reactive Energy
		T2_TOTAL_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		T2_FORWARD_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		T2_REVERSE_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //

		// T3 Active Energy
		T3_TOTAL_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		T3_FORWARD_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		T3_REVERSE_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //

		// T3 Reactive Energy
		T3_TOTAL_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		T3_FORWARD_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		T3_REVERSE_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //

		// T4 Active Energy
		T4_TOTAL_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		T4_FORWARD_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //
		T4_REVERSE_ACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT_HOURS)), //

		// T4 Reactive Energy
		T4_TOTAL_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		T4_FORWARD_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		T4_REVERSE_REACTIVE_ENERGY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS));

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