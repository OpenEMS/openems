package io.openems.edge.kostal.ess;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.kostal.enums.BatteryManagementMode;
import io.openems.edge.kostal.enums.BatteryType;
import io.openems.edge.kostal.enums.EnergyManagerMode;
import io.openems.edge.kostal.enums.FuseState;
import io.openems.edge.kostal.enums.InverterState;
import io.openems.edge.kostal.enums.SensorType;

public interface KostalManagedESS
		extends
			ManagedSymmetricEss,
			SymmetricEss,
			ModbusComponent,
			OpenemsComponent {
	public static enum ChannelId
			implements
				io.openems.edge.common.channel.ChannelId {
		// EnumReadChannels
		INVERTER_STATE(Doc.of(InverterState.values())), //
		FUSE_STATE(Doc.of(FuseState.values())), //
		ENERGY_MANAGER_MODE(Doc.of(EnergyManagerMode.values())), //
		OPERATING_MODE_FOR_BATTERY_MANAGEMENT(
				Doc.of(BatteryManagementMode.values())), //

		// EnumWriteChannsl
		BATTERY_TYPE(Doc.of(BatteryType.values())), //
		SENSOR_TYPE(Doc.of(SensorType.values())), //

		// LongReadChannels
		SERIAL_NUMBER(Doc.of(OpenemsType.LONG)
				.persistencePriority(PersistencePriority.HIGH) //
		), //

		// IntegerWriteChannels
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.VOLT_AMPERE)), //

		CURRENT_BATTERY_CAPACITY(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
		), //

		BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT) //
		), //
		BATTERY_TEMPERATURE(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS) //
		), //
		BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE) //
		);

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
