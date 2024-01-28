package io.openems.edge.evcs.evtec;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface EvcsEvTec extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATION_STATE(Doc.of(StationState.values()) //
				.persistencePriority(PersistencePriority.HIGH).text("Station State")), //
		CHARGING_STATE(Doc.of(ChargingState.values()) //
				.persistencePriority(PersistencePriority.HIGH).text("Charge state")), //
		VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW).text("Voltage")), //
		CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW).text("Current")),
		POWER_UINT(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Power UInt")), //
		CONNECTOR_TYPE(Doc.of(ConnectorType.values()) //
				.persistencePriority(PersistencePriority.VERY_LOW).text("Connector Type")), //
		CHARGE_TIME(Doc.of(OpenemsType.FLOAT).unit(Unit.SECONDS) //
				.persistencePriority(PersistencePriority.LOW).text("Charge time")), //
		CHARGED_ENERGY(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.MEDIUM).text("Charged energy")), //
		DISCHARGED_ENERGY(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.MEDIUM).text("Discharged energy")), //
		L_LIMIT_POTENTIAL(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Lower limit potential")), //
		L_LIMIT_POTENTIAL_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Lower limit potential L1")), //
		L_LIMIT_POTENTIAL_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Lower limit potential L2")), //
		L_LIMIT_POTENTIAL_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Lower limit potential L3")), //
		U_LIMIT_REQUEST(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Upper limit reqeust")), //
		U_LIMIT_REQUEST_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Upper limit reqeust L1")), //
		U_LIMIT_REQUEST_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Upper limit reqeust L2")), //
		U_LIMIT_REQUEST_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Upper limit reqeust L3")), //
		L_LIMIT_REQUEST(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Lower limit reqeust")), //
		L_LIMIT_REQUEST_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Lower limit reqeust L1")), //
		L_LIMIT_REQUEST_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Lower limit reqeust L2")), //
		L_LIMIT_REQUEST_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Lower limit reqeust L3")), //
		PRESENT_CONSUMPTION(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Present consumption total")), //
		PRESENT_CONSUMPTION_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Present consumption L1")), //
		PRESENT_CONSUMPTION_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Present consumption L2")), //
		PRESENT_CONSUMPTION_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW).text("Present consumption L3")), //
		ERROR(Doc.of(Level.FAULT)), TOTAL_BATTERY_CAPACITY(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW).text("Capacity of the battery")), //
		REMAINING_BATTERY_CAPACITY(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW).text("Remaining capacity of the battery")), //
		MINIMAL_BATTERY_CAPACITY(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW).text("Minimal capacity of the battery")), //
		BULK_CHARGE_CAPACITY(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW).text("Bulk charge capacity")), //
		RFID(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.VERY_LOW).text("RFID")), //
		EVCC_ID(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.VERY_LOW).text("EVCC ID")), //
		SUSPEND_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.LOW) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("true if charging pause")), //
		INPUT_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.accessMode(AccessMode.READ_WRITE).text("input power")), //

		/**
		 * Maximum Discharge Power defined by software.
		 *
		 * <ul>
		 * <li>Interface: Evcs
		 * <li>Readable
		 * <li>Type: Integer - positive value only
		 * <li>Unit: W
		 * </ul>
		 */
		MAXIMUM_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //
		COULD_NOT_READ_CHARGING_STATE(Doc.of(Level.WARNING) //
				.text("Could not read charging state."))

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

	public default FloatReadChannel getVoltageChannel() {
		return this.channel(ChannelId.VOLTAGE);
	}

	public default Value<Float> getVoltage() {
		return this.getVoltageChannel().value();
	}

	public default FloatReadChannel getChargedEnergyChannel() {
		return this.channel(ChannelId.CHARGED_ENERGY);
	}

	public default Value<Float> getChargedEnergy() {
		return this.getChargedEnergyChannel().value();
	}

	public default FloatReadChannel getDischargedEnergyChannel() {
		return this.channel(ChannelId.DISCHARGED_ENERGY);
	}

	public default Value<Float> getDischargedEnergy() {
		return this.getDischargedEnergyChannel().value();
	}

	public default IntegerWriteChannel getInputPowerChannel() {
		return this.channel(ChannelId.INPUT_POWER);
	}

	public default Value<Integer> getInputPower() {
		return this.getInputPowerChannel().value();
	}

	/**
	 * set input power write channel.
	 *
	 * @param value the power
	 */
	public default void setInputPower(int value) throws OpenemsNamedException {
		this.getInputPowerChannel().setNextWriteValue(value);
	}

	public default IntegerReadChannel getMaximumDischargePowerChannel() {
		return this.channel(ChannelId.MAXIMUM_DISCHARGE_POWER);
	}

	public default Value<Integer> getMaximumDischargePower() {
		return this.getMaximumDischargePowerChannel().value();
	}

	/**
	 * set max discharge power.
	 *
	 * @param value the max discharge power
	 */
	public default void _setMaximumDischargePower(Integer value) {
		this.getMaximumDischargePowerChannel().setNextValue(value);
	}

	public default BooleanWriteChannel getSuspendModeChannel() {
		return this.channel(ChannelId.SUSPEND_MODE);
	}

}