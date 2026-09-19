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
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.TimedataProvider;

/**
 * Interface for the Alfen NG9xx charging station (Eve Single S-line, Eve Single
 * Pro-line, Eve Double Pro-line).
 *
 * <p>
 * Values that have a matching {@link ElectricityMeter} Channel - voltages,
 * currents, frequency, active power and energy - are mapped directly to that
 * Channel and are therefore intentionally missing here.
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
		APPARENT_POWER_SUM(Doc.of(FLOAT)//
				.unit(Unit.VOLT_AMPERE)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Apparent Power sum")), //
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
		MODE_3_STATE(Doc.of(Mode3State.values())//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Mode 3 state")), //
		ACTUAL_APPLIED_MAX_CURRENT(Doc.of(FLOAT)//
				.unit(Unit.AMPERE)//
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
	 * Gets the Channel for {@link ChannelId#MODE_3_STATE}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getMode3StateChannel() {
		return this.channel(ChannelId.MODE_3_STATE);
	}

	/**
	 * Gets the {@link Mode3State}.
	 *
	 * @return the {@link Mode3State}
	 */
	public default Mode3State getMode3State() {
		return this.getMode3StateChannel().value().asEnum();
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
	 * Gets the currently configured phase mode. See {@link ChannelId#SET_PHASES}.
	 *
	 * @return the Channel {@link Value}; 1 or 3 phases
	 */
	public default Value<Integer> getSetPhases() {
		return this.getSetPhasesChannel().value();
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
