package io.openems.edge.evse.chargepoint.ambibox;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Level.FAULT;
import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.channel.Unit.DEGREE_CELSIUS;
import static io.openems.common.channel.Unit.PERCENT;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.channel.Unit.VOLT;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.chargepoint.ambibox.enums.BatteryState;
import io.openems.edge.evse.chargepoint.ambibox.enums.ChargeProtocol;
import io.openems.edge.evse.chargepoint.ambibox.enums.ControlMode;
import io.openems.edge.evse.chargepoint.ambibox.enums.EvseState;
import io.openems.edge.evse.chargepoint.ambibox.enums.InverterState;
import io.openems.edge.meter.api.ElectricityMeter;

public abstract class EvseChargePointAmbibox extends AbstractOpenemsModbusComponent
		implements OpenemsComponent, ElectricityMeter {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Charger is asleep; see register 'Sleep'.
		 */
		SLEEP(Doc.of(BOOLEAN)), //

		POWER_FACTOR(Doc.of(FLOAT)), //

		VOLTAGE_DC(Doc.of(FLOAT)//
				.unit(VOLT)), //

		CURRENT_DC(Doc.of(FLOAT)//
				.unit(AMPERE)), //

		POWER_DC(Doc.of(INTEGER)//
				.unit(WATT)), //

		/**
		 * Number of active AC phases as reported by the charger.
		 */
		NUMBER_PHASES(Doc.of(INTEGER)), //

		/**
		 * Minimum PowerAC of the charger; see register 'Minimum PowerAC'.
		 */
		CHARGER_MIN_POWER(Doc.of(INTEGER)//
				.unit(WATT)), //

		/**
		 * Maximum PowerAC of the charger; see register 'Maximum PowerAC'.
		 */
		CHARGER_MAX_POWER(Doc.of(INTEGER)//
				.unit(WATT)), //

		INVERTER_STATE(Doc.of(InverterState.values())), //

		/**
		 * Inverter Error; bit field, see protocol documentation 'Inverter Errors'.
		 */
		INVERTER_ERROR(Doc.of(INTEGER)), //

		INVERTER_TEMPERATURE(Doc.of(FLOAT)//
				.unit(DEGREE_CELSIUS)), //

		/**
		 * Capacity of the EV battery in [Wh].
		 */
		CAPACITY(Doc.of(INTEGER)//
				.unit(WATT_HOURS)), //

		MIN_SOC(Doc.of(INTEGER)//
				.unit(PERCENT)), //

		MAX_SOC(Doc.of(INTEGER)//
				.unit(PERCENT)), //

		/**
		 * State of Charge of the EV battery in [%].
		 */
		EV_SOC(Doc.of(INTEGER)//
				.unit(PERCENT)), //

		STATE_OF_HEALTH(Doc.of(INTEGER)//
				.unit(PERCENT)), //

		TIME_TO_FULL_SOC(Doc.of(INTEGER)//
				.unit(SECONDS)), //

		NUMBER_CYCLES(Doc.of(INTEGER)), //

		/**
		 * Minimum ChargePower of the EV battery in [W]; see register 'Min
		 * ChargePower'.
		 */
		EV_MIN_CHARGE_POWER(Doc.of(INTEGER)//
				.unit(WATT)), //

		/**
		 * Maximum ChargePower of the EV battery in [W]; see register 'Max
		 * ChargePower'.
		 */
		EV_MAX_CHARGE_POWER(Doc.of(INTEGER)//
				.unit(WATT)), //

		/**
		 * Minimum DischargePower of the EV battery in [W]; see register 'Min
		 * DischargePower'. Discharge management is not implemented.
		 */
		EV_MIN_DISCHARGE_POWER(Doc.of(INTEGER)//
				.unit(WATT)), //

		/**
		 * Maximum DischargePower of the EV battery in [W]; see register 'Max
		 * DischargePower'. Discharge management is not implemented.
		 */
		EV_MAX_DISCHARGE_POWER(Doc.of(INTEGER)//
				.unit(WATT)), //

		BATTERY_STATE(Doc.of(BatteryState.values())), //

		BATTERY_TEMPERATURE(Doc.of(FLOAT)//
				.unit(DEGREE_CELSIUS)), //

		/**
		 * Battery Error; bit field, see protocol documentation 'Battery Error'.
		 */
		BATTERY_ERROR(Doc.of(INTEGER)), //

		CONTROL_MODE(Doc.of(ControlMode.values())), //

		/**
		 * Power DC of the EV battery in [W]; positive value = charging the EV.
		 */
		POWER_DC_BATTERY(Doc.of(INTEGER)//
				.unit(WATT)), //

		CHARGE_PROTOCOL(Doc.of(ChargeProtocol.values())), //

		/**
		 * Session State of the charger; see protocol documentation 'Session
		 * State'.
		 */
		SESSION_STATE(Doc.of(EvseState.values())//
				.persistencePriority(HIGH)), //

		/**
		 * An Electric-Vehicle is connected.
		 */
		EV_CONNECTED(Doc.of(BOOLEAN)//
				.persistencePriority(HIGH)), //

		SECONDS_TO_DEPARTURE(Doc.of(INTEGER)//
				.unit(SECONDS)), //

		DEPARTURE_SOC(Doc.of(INTEGER)//
				.unit(PERCENT)), //

		DEPARTURE_ENERGY(Doc.of(INTEGER)//
				.unit(WATT_HOURS)), //

		MIN_ENERGY_REQUEST(Doc.of(INTEGER)//
				.unit(WATT_HOURS)), //

		MAX_ENERGY_REQUEST(Doc.of(INTEGER)//
				.unit(WATT_HOURS)), //

		/**
		 * EvCharger Error; bit field, see protocol documentation 'EvCharger
		 * Error'. The individual bits are mapped to the ERR_* channels.
		 */
		EV_CHARGER_ERROR(Doc.of(INTEGER)), //

		ERR_EV_COMMUNICATION(Doc.of(FAULT)), //
		ERR_PE_COMMUNICATION(Doc.of(FAULT)), //
		ERR_LOAD_DUMP(Doc.of(FAULT)), //
		ERR_PLUG_TEMPERATURE(Doc.of(FAULT)), //
		ERR_CABLE_CHECK(Doc.of(FAULT)), //
		ERR_EMERGENCY_OFF(Doc.of(FAULT)), //

		/**
		 * Imported Energy of the current charging session in [Wh].
		 */
		ENERGY_SESSION(Doc.of(INTEGER)//
				.unit(WATT_HOURS)), //

		/**
		 * Exported Energy of the current charging session in [Wh]. Discharge
		 * management is not implemented.
		 */
		ENERGY_EXPORT_SESSION(Doc.of(INTEGER)//
				.unit(WATT_HOURS)), //

		/**
		 * A replug of the Electric-Vehicle is required.
		 */
		REPLUG_REQUIRED(Doc.of(BOOLEAN)), //

		/**
		 * Target Power set-point in [W]; positive value = charging the EV. The
		 * sign is inverted when writing the Modbus 'Target PowerAC' register,
		 * i.e. negative register values charge the vehicle.
		 */
		SET_TARGET_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.accessMode(READ_WRITE)//
				.persistencePriority(HIGH)), //

		/**
		 * WriteChannel for the 'Wake Up' Holding Register; writing 1 wakes up
		 * the charger.
		 */
		SET_WAKE_UP(Doc.of(INTEGER)//
				.accessMode(WRITE_ONLY)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	protected EvseChargePointAmbibox(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SLEEP}.
	 *
	 * @return the Channel
	 */
	public Channel<Boolean> getSleepChannel() {
		return this.channel(ChannelId.SLEEP);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SESSION_STATE}.
	 *
	 * @return the Channel
	 */
	public Channel<EvseState> getSessionStateChannel() {
		return this.channel(ChannelId.SESSION_STATE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EV_CONNECTED}.
	 *
	 * @return the Channel
	 */
	public Channel<Boolean> getEvConnectedChannel() {
		return this.channel(ChannelId.EV_CONNECTED);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EV_CHARGER_ERROR}.
	 *
	 * @return the Channel
	 */
	public Channel<Integer> getEvChargerErrorChannel() {
		return this.channel(ChannelId.EV_CHARGER_ERROR);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_TARGET_POWER}.
	 *
	 * @return the Channel
	 */
	public IntegerWriteChannel getSetTargetPowerChannel() {
		return this.channel(ChannelId.SET_TARGET_POWER);
	}

	/**
	 * Sets the Target Power in [W] on {@link ChannelId#SET_TARGET_POWER} Channel.
	 * Positive value = charging the EV.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public void setSetTargetPower(Integer value) throws OpenemsNamedException {
		this.getSetTargetPowerChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_WAKE_UP}.
	 *
	 * @return the Channel
	 */
	public IntegerWriteChannel getSetWakeUpChannel() {
		return this.channel(ChannelId.SET_WAKE_UP);
	}

	/**
	 * Wakes up the charger via {@link ChannelId#SET_WAKE_UP} Channel.
	 *
	 * @throws OpenemsNamedException on error
	 */
	public void setWakeUp() throws OpenemsNamedException {
		this.getSetWakeUpChannel().setNextWriteValue(1);
	}
}
