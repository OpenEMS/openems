package io.openems.edge.batteryinverter.victron.ro;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.victron.ro.statemachine.StateMachine.State;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.victron.enums.ActiveInactive;
import io.openems.edge.victron.enums.ActiveInputSource;
import io.openems.edge.victron.enums.BatteryState;

public interface VictronBatteryInverter extends OffGridBatteryInverter,
		ManagedSymmetricBatteryInverter, SymmetricBatteryInverter,
		OpenemsComponent, StartStoppable, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)), //

		CCGX_RELAY1_STATE(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.ON_OFF)), //
		CCGX_RELAY2_STATE(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.ON_OFF)), //
		AC_PV_ON_OUTPUT_POWER_L1(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		AC_PV_ON_OUTPUT_POWER_L2(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		AC_PV_ON_OUTPUT_POWER_L3(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		AC_PV_ON_INPUT_POWER_L1(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		AC_PV_ON_INPUT_POWER_L2(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		AC_PV_ON_INPUT_POWER_L3(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		AC_CONSUMPTION_POWER_L1(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		AC_CONSUMPTION_POWER_L2(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		AC_CONSUMPTION_POWER_L3(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		GRID_POWER_L1(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		GRID_POWER_L2(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		GRID_POWER_L3(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		AC_GENSET_POWER_L1(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		AC_GENSET_POWER_L2(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		AC_GENSET_POWER_L3(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		ACTIVE_INPUT_SOURCE(Doc.of(ActiveInputSource.values())
				.accessMode(AccessMode.READ_ONLY)),
		DC_BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT)),
		DC_BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.AMPERE)),
		BATTERY_SOC(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)
				.unit(Unit.PERCENT)),
		BATTERY_STATE(
				Doc.of(BatteryState.values()).accessMode(AccessMode.READ_ONLY)),
		BATTERY_CONSUMED_AMPHOURS(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.AMPERE_HOURS)),
		BATTERY_TIME_TO_GO(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.SECONDS)),
		DC_PV_POWER(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)
				.unit(Unit.WATT)),
		DC_PV_CURRENT(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.AMPERE)),
		CHARGER_POWER(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		DC_SYSTEM_POWER(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		VE_BUS_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.AMPERE)),
		VE_BUS_CHARGE_POWER(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)),
		ESS_CONTROL_LOOP_SETPOINT(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_WRITE).unit(Unit.WATT)),
		ESS_MAX_CHARGE_CURRENT_PERCENTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT)),
		ESS_MAX_DISCHARGE_CURRENT_PERCENTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT)),
		ESS_CONTROL_LOOP_SETPOINT_SCALE_FACTOR_2(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_WRITE).unit(Unit.WATT)),
		ESS_MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_WRITE).unit(Unit.WATT)),
		SYSTEM_MAX_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_WRITE).unit(Unit.AMPERE)),
		MAX_FEED_IN_POWER(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_WRITE).unit(Unit.WATT)),
		FEED_EXCESS_DC(
				Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
		DONT_FEED_EXCESS_AC(
				Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
		PV_POWER_LIMITER_ACTIVE(Doc.of(ActiveInactive.values())
				.accessMode(AccessMode.READ_ONLY)),
		MAX_CHARGE_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_WRITE).unit(Unit.VOLT)),
		SET_ACTIVE_POWER_L1(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE)),
		SET_ACTIVE_POWER_L2(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE)),
		SET_ACTIVE_POWER_L3(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE))
		; //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public enum Type {
		Multiplus2GX3kVa("Multiplus II-GX 3kVA Single Phase", -2400, 2400,
				2400),
		Multiplus2GX5kVa("Multiplus II-GX 5kVA Single Phase", -4000, 4000,
				4000),
		Multiplus2GX3kVaL1L2L3("Multiplus II-GX 3kVA Three Phase System",
				-2400 * 3, 2400 * 3, 2400 * 3),
		Multiplus2GX5kVaL1L2L3("Multiplus II-GX 5kVA Three Phase System",
				-4000 * 3, 4000 * 3, 4000 * 3);

		private int acInputLimit;
		private int acOutputLimit;
		private String displayName;
		private int apparentPowerLimit;

		Type(String displayName, int acInputLimit, int acOutputLimit,
				int apparentPowerLimit) {
			this.displayName = displayName;
			this.acInputLimit = acInputLimit;
			this.acOutputLimit = acOutputLimit;
			this.apparentPowerLimit = apparentPowerLimit;
		}

		public int getAcInputLimit() {
			return acInputLimit;
		}

		public int getAcOutputLimit() {
			return acOutputLimit;
		}

		public String getDisplayName() {
			return displayName;
		}

		public int getApparentPowerLimit() {
			return apparentPowerLimit;
		}
	}

}
