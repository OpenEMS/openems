package io.openems.edge.evse.chargepoint.heidelberg.connect;

import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.chargepoint.heidelberg.connect.enums.ChargingState;
import io.openems.edge.evse.chargepoint.heidelberg.connect.enums.LockState;
import io.openems.edge.evse.chargepoint.heidelberg.connect.enums.PhaseSwitchControl;
import io.openems.edge.evse.chargepoint.heidelberg.connect.enums.ReadyForCharging;

public interface EvseChargePointHeidelbergConnect extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		CHARGING_STATE(Doc.of(ChargingState.values())),

		DEBUG_SET_CHARGING_CURRENT(Doc.of(INTEGER) //
				.unit(MILLIAMPERE)), //
		SET_CHARGING_CURRENT(Doc.of(INTEGER) //
				.unit(MILLIAMPERE) //
				.accessMode(WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_CHARGING_CURRENT)),

		READY_FOR_CHARGING(Doc.of(ReadyForCharging.values()) //
				.text("Wallox is ready for charging")),

		// decimal 264 -> hexadecimal 0x108 -> Version V1.0.8
		LAYOUT_VERSION(Doc.of(OpenemsType.INTEGER)),

		TEMPERATURE_PCB(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		/**
		 * This register represents the status of the input for external lock (see
		 * manual).
		 * 
		 * <p>
		 * 0 = system locked 1 = system unlocked TODO: Check when the state is 1
		 */
		EXTERN_LOCK_STATE(Doc.of(LockState.values())), //

		RAW_MAXIMAL_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)),

		RAW_MINIMAL_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)),

		/**
		 * WatchDog TimeOut for the Modbus TCP Leader.
		 * 
		 * <p>
		 * Within this period, at least one successful Modbus TCP communication must
		 * have taken place between the Modbus TCP Leader and the Modbus TCP Follower.
		 * Otherwise, the Modbus TCP Follower goes into TimeOut mode.
		 * 
		 * <p>
		 * Default timeout: 15 seconds
		 */
		WATCHDOG_TIMEOUT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE).unit(Unit.MILLISECONDS)),

		// Currently unused
		REMOTE_LOCK(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)),

		HEIDELBERG_ENERGY_SESSION(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT_HOURS)),

		FAILSAFE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),

		PHASE_SWITCH_CONTROL(Doc.of(PhaseSwitchControl.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Currently used phases")),

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

	/**
	 * Gets the Channel for {@link ChannelId#SET_CHARGING_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetChargingCurrentChannel() {
		return this.channel(ChannelId.SET_CHARGING_CURRENT);
	}

	/**
	 * Sets the write value of the {@link ChannelId#SET_CHARGING_CURRENT} Channel
	 * used to set the charge current limit of the Charge-Point in [mA].
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setChargingCurrent(Integer value) throws OpenemsNamedException {
		this.getSetChargingCurrentChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_SWITCH_CONTROL}.
	 *
	 * @return the Channel
	 */
	public default EnumWriteChannel getPhaseSwitchControlChannel() {
		return this.channel(ChannelId.PHASE_SWITCH_CONTROL);
	}

	/**
	 * Gets the read value for Channel {@link ChannelId#PHASE_SWITCH_CONTROL}.
	 *
	 * @return the Channel read value
	 */
	public default PhaseSwitchControl getPhaseSwitchControl() {
		return this.getPhaseSwitchControlChannel().getNextValue().asEnum();
	}

	/**
	 * Sets the write value of the {@link ChannelId#PHASE_SWITCH_CONTROL} Channel
	 * used to set the charging phases to 1 or 3 phase charging.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setPhaseSwitchControl(PhaseSwitchControl value) throws OpenemsNamedException {
		this.getPhaseSwitchControlChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#READY_FOR_CHARGING}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getReadyForChargingChannel() {
		return this.channel(ChannelId.READY_FOR_CHARGING);
	}
}
