package io.openems.edge.sma.sunnyisland;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.sma.enums.OperatingModeForActivePowerLimitation;
import io.openems.edge.sma.enums.PowerSupplyStatus;
import io.openems.edge.sma.enums.SetControlMode;
import io.openems.edge.sma.enums.SystemState;

public interface SunnyIslandEss extends ManagedSinglePhaseEss, SinglePhaseEss, ManagedAsymmetricEss, AsymmetricEss,
		ManagedSymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// EnumReadChannels
		SYSTEM_STATE(Doc.of(SystemState.values())), //
		POWER_SUPPLY_STATUS(Doc.of(PowerSupplyStatus.values())), //
		OPERATING_MODE_FOR_ACTIVE_POWER_LIMITATION(Doc.of(OperatingModeForActivePowerLimitation.values())), //

		// EnumWriteChannsl
		SET_CONTROL_MODE(Doc.of(SetControlMode.values()).accessMode(AccessMode.READ_WRITE)), //

		// LongReadChannels
		SERIAL_NUMBER(Doc.of(OpenemsType.LONG) //
				.persistencePriority(PersistencePriority.HIGH)), //

		// IntegerWriteChannels
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.VOLT_AMPERE)), //
		MIN_SOC_POWER_ON(Doc.of(OpenemsType.INTEGER)), //
		GRID_GUARD_CODE(Doc.of(OpenemsType.INTEGER)), //
		MIN_SOC_POWER_OFF(Doc.of(OpenemsType.INTEGER)), //

		// IntegerReadChannels
		DEVICE_CLASS(Doc.of(OpenemsType.INTEGER)), //
		DEVICE_TYPE(Doc.of(OpenemsType.INTEGER)), //
		SOFTWARE_PACKAGE(Doc.of(OpenemsType.INTEGER)), //
		WAITING_TIME_UNTIL_FEED_IN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS)), //
		MESSAGE(Doc.of(OpenemsType.INTEGER)), //
		RECOMMENDED_ACTION(Doc.of(OpenemsType.INTEGER)), //
		FAULT_CORRECTION_MEASURE(Doc.of(OpenemsType.INTEGER)), //
		GRID_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		GRID_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		GRID_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIHERTZ)), //
		CURRENT_BATTERY_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)), //

		LOWEST_MEASURED_BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		HIGHEST_MEASURED_BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		MAX_OCCURRED_BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //

		BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Specify implementation to apply the calculated Power.
	 *
	 * @param activePowerL1   the active power set-point for L1
	 * @param reactivePowerL1 the reactive power set-point for L1
	 * @param activePowerL2   the active power set-point for L2
	 * @param reactivePowerL2 the reactive power set-point for L2
	 * @param activePowerL3   the active power set-point for L3
	 * @param reactivePowerL3 the reactive power set-point for L3
	 */
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException;
}