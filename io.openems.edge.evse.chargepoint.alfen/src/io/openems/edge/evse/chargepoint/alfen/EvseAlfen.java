package io.openems.edge.evse.chargepoint.alfen;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.TimedataProvider;

/**
 * Interface for the Alfen NG9xx charging station (Eve Single S-line, Eve Single
 * Pro-line, Eve Double Pro-line).
 */
public interface EvseAlfen extends EvseChargePoint, ElectricityMeter, OpenemsComponent, TimedataProvider {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		METER_STATE(Doc.of(INTEGER)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Meter state")), //
		METER_LAST_VALUE_TIMESTAMP(Doc.of(INTEGER)//
				.unit(Unit.MILLISECONDS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Milliseconds since last received measurement")), //
		METER_TYPE(Doc.of(INTEGER)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Meter type")), //
		VOLTAGE_L1_RAW(Doc.of(FLOAT)//
				.unit(Unit.VOLT)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Voltage Phase L1 (raw)")), //
		VOLTAGE_L2_RAW(Doc.of(FLOAT)//
				.unit(Unit.VOLT)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Voltage Phase L2 (raw)")), //
		VOLTAGE_L3_RAW(Doc.of(FLOAT)//
				.unit(Unit.VOLT)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Voltage Phase L3 (raw)")), //
		CURRENT_L1_RAW(Doc.of(FLOAT)//
				.unit(Unit.MILLIAMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Current as metered on Input Phase L1")), //
		CURRENT_L2_RAW(Doc.of(FLOAT)//
				.unit(Unit.MILLIAMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Current as metered on Input Phase L2")), //
		CURRENT_L3_RAW(Doc.of(FLOAT)//
				.unit(Unit.MILLIAMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Current as metered on Input Phase L3")), //
		CURRENT_N(Doc.of(FLOAT)//
				.unit(Unit.AMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Current N")), //
		POWER_FACTOR_L1(Doc.of(FLOAT)//
				.unit(Unit.NONE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Power factor L1")), //
		POWER_FACTOR_L2(Doc.of(FLOAT)//
				.unit(Unit.NONE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Power factor L2")), //
		POWER_FACTOR_L3(Doc.of(FLOAT)//
				.unit(Unit.NONE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Power factor L3")), //
		POWER_FACTOR_SUM(Doc.of(FLOAT)//
				.unit(Unit.NONE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Power factor sum")), //
		CHARGE_POWER_L1(Doc.of(FLOAT)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Charge Power L1")), //
		CHARGE_POWER_L2(Doc.of(FLOAT)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Charge Power L2")), //
		CHARGE_POWER_L3(Doc.of(FLOAT)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Charge Power L3")), //
		CHARGE_POWER(Doc.of(FLOAT)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Charge Power Total")), //
		APPARENT_POWER_SUM(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Apparent Power sum")), //
		REACTIVE_POWER_SUM(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Reactive Power sum")), //
		// Line-to-line voltages from PDF register map section 3.4
		VOLTAGE_L1_L2(Doc.of(FLOAT)//
				.unit(Unit.VOLT)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Voltage Phase L1-L2")), //
		VOLTAGE_L2_L3(Doc.of(FLOAT)//
				.unit(Unit.VOLT)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Voltage Phase L2-L3")), //
		VOLTAGE_L3_L1(Doc.of(FLOAT)//
				.unit(Unit.VOLT)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Voltage Phase L3-L1")), //
		// Current sum from PDF register 326-327
		CURRENT_SUM(Doc.of(FLOAT)//
				.unit(Unit.AMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Current Sum")), //
		// Apparent power per phase from PDF register 346-351
		APPARENT_POWER_L1(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Apparent Power L1")), //
		APPARENT_POWER_L2(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Apparent Power L2")), //
		APPARENT_POWER_L3(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Apparent Power L3")), //
		// Note: Reactive Power L1/L2/L3 are mapped directly to ElectricityMeter
		// channels
		// Energy delivered per phase from PDF register 362-373
		ENERGY_DELIVERED_L1(Doc.of(FLOAT)//
				.unit(Unit.WATT_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Real energy delivered L1")), //
		ENERGY_DELIVERED_L2(Doc.of(FLOAT)//
				.unit(Unit.WATT_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Real energy delivered L2")), //
		ENERGY_DELIVERED_L3(Doc.of(FLOAT)//
				.unit(Unit.WATT_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Real energy delivered L3")), //
		ENERGY_DELIVERED_SUM(Doc.of(FLOAT)//
				.unit(Unit.WATT_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Real energy delivered sum")), //
		// Energy consumed per phase + sum from PDF register 378-393
		ENERGY_CONSUMED_L1(Doc.of(FLOAT)//
				.unit(Unit.WATT_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Real energy consumed L1")), //
		ENERGY_CONSUMED_L2(Doc.of(FLOAT)//
				.unit(Unit.WATT_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Real energy consumed L2")), //
		ENERGY_CONSUMED_L3(Doc.of(FLOAT)//
				.unit(Unit.WATT_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Real energy consumed L3")), //
		ENERGY_CONSUMED_SUM(Doc.of(FLOAT)//
				.unit(Unit.WATT_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Real energy consumed sum")), //
		// Apparent energy per phase + sum from PDF register 394-409
		APPARENT_ENERGY_L1(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Apparent energy L1")), //
		APPARENT_ENERGY_L2(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Apparent energy L2")), //
		APPARENT_ENERGY_L3(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Apparent energy L3")), //
		APPARENT_ENERGY_SUM(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Apparent energy sum")), //
		// Reactive energy per phase + sum from PDF register 410-425
		REACTIVE_ENERGY_L1(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Reactive energy L1")), //
		REACTIVE_ENERGY_L2(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Reactive energy L2")), //
		REACTIVE_ENERGY_L3(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Reactive energy L3")), //
		REACTIVE_ENERGY_SUM(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Reactive energy sum")), //
		AVAILABILITY(Doc.of(OpenemsType.BOOLEAN)//
				.persistencePriority(PersistencePriority.MEDIUM)//
				.text("Availability")), //
		/**
		 * See Modbus specification for details on the Mode 3 state.
		 */
		MODE_3_STATE(Doc.of(OpenemsType.STRING)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Mode 3 state")), //
		ACTUAL_APPLIED_MAX_CURRENT(Doc.of(FLOAT)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Actual applied max current")), //
		MODBUS_SLAVE_MAX_CURRENT_VALID_TIME(Doc.of(INTEGER)//
				.unit(Unit.SECONDS)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Remaining time before fallback to safe current")), //
		DEBUG_SET_CURRENT(Doc.of(FLOAT)//
				.unit(Unit.AMPERE)), //
		SET_CURRENT(Doc.of(FLOAT)//
				.accessMode(READ_WRITE)//
				.unit(Unit.AMPERE)//
				.persistencePriority(PersistencePriority.MEDIUM)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_SET_CURRENT)//
				.text("Modbus slave max current")), //
		ACTIVE_LOAD_BALANCING_SAFE_CURRENT(Doc.of(FLOAT)//
				.unit(Unit.AMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Active load balancing safe current")), //
		MODBUS_SLAVE_RECEIVED_SETPOINT_ACCOUNTED_FOR(Doc.of(OpenemsType.BOOLEAN)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Modbus slave received setpoint accounted for")), //
		DEBUG_SET_PHASES(Doc.of(INTEGER)), //
		// READ_WRITE: register 1215 is also read back as the source of truth for the
		// currently configured phase mode (see getPhases())
		SET_PHASES(Doc.of(INTEGER)//
				.accessMode(READ_WRITE)//
				.unit(Unit.NONE)//
				.persistencePriority(PersistencePriority.MEDIUM)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_SET_PHASES)//
				.text("Charge using 1 or 3 phases")), //
		ERROR(Doc.of(Level.FAULT).persistencePriority(PersistencePriority.HIGH)//
				.text("Error in the charging station.")//
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

	/**
	 * Gets the Channel for {@link ChannelId#SET_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default FloatWriteChannel getSetCurrentChannel() {
		return this.channel(ChannelId.SET_CURRENT);
	}

	/**
	 * Sets the next Write Value for {@link ChannelId#SET_CURRENT}.
	 *
	 * @param value the value in Ampere
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetCurrent(float value) throws OpenemsNamedException {
		this.getSetCurrentChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_PHASES}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetPhasesChannel() {
		return this.channel(ChannelId.SET_PHASES);
	}

	/**
	 * Sets the next Write Value for {@link ChannelId#SET_PHASES}.
	 *
	 * @param value 1 or 3 phases
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetPhases(int value) throws OpenemsNamedException {
		this.getSetPhasesChannel().setNextWriteValue(value);
	}
}
