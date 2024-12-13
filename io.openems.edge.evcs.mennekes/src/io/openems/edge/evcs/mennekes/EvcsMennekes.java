package io.openems.edge.evcs.mennekes;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Mennekes Amtron Professional charging protocol interface.
 * 
 * <p>
 * Defines the interface for Mennekes Amtron Professional
 */
public interface EvcsMennekes extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Apply charge power limit.
		 * 
		 * <p>
		 * WriteChannel for the modbus register to apply the charge power given by the
		 * applyChargePowerLimit method
		 */
		APPLY_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //

		FIRMWARE_VERSION(Doc.of(OpenemsType.STRING)//
				.unit(Unit.NONE)//
				.text("Readable Firmware Version")),

		FIRMWARE_OUTDATED(Doc.of(Level.INFO)//
				.translationKey(EvcsMennekes.class, "firmwareOutdated")),

		RAW_FIRMWARE_VERSION(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)//
				.text("Firmware Version as Long; has to converted to hex to interpret")), //

		OCPP_CP_STATUS(Doc.of(MennekesOcppState.values())//
				.persistencePriority(PersistencePriority.HIGH)//
		), //

		/**
		 * ERR_RCMB_TRIGGERED.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_RCMB_TRIGGERED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errRcmbTriggered")), //

		/**
		 * ERR_VEHICLE_STATE_E.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_VEHICLE_STATE_E(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errVehicleStateE")), //

		/**
		 * ERR_MODE3_DIODE_CHECK.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_MODE3_DIODE_CHECK(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errMode3DiodeCheck")), //

		/**
		 * ERR_MCB_TYPE2_TRIGGERED.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_MCB_TYPE2_TRIGGERED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errMcbType2Triggered")), //

		/**
		 * ERR_MCB_SCHUKO_TRIGGERED.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_MCB_SCHUKO_TRIGGERED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errMcbSchukoTriggered")), //

		/**
		 * ERR_RCD_TRIGGERED.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_RCD_TRIGGERED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errRcdTriggered")), //

		/**
		 * ERR_CONTACTOR_WELD.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_CONTACTOR_WELD(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errContactorWeld")), //

		/**
		 * ERR_BACKEND_DISCONNECTED.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_BACKEND_DISCONNECTED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errBackendDisconnected")), //

		/**
		 * ERR_ACTUATOR_LOCKING_FAILED.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_ACTUATOR_LOCKING_FAILED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errActuatorLockingFailed")), //

		/**
		 * ERR_ACTUATOR_LOCKING_WITHOUT_PLUG_FAILED.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_ACTUATOR_LOCKING_WITHOUT_PLUG_FAILED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errActuatorLockingWithoutPlugFailed")), //

		/**
		 * ERR_ACTUATOR_STUCK.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_ACTUATOR_STUCK(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errActuatorStuck")), //

		/**
		 * ERR_ACTUATOR_DETECTION_FAILED.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_ACTUATOR_DETECTION_FAILED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errActuatorDetectionFailed")), //

		/**
		 * ERR_FW_UPDATE_RUNNING.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_FW_UPDATE_RUNNING(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errFwUpdateRunning")), //

		/**
		 * ERR_TILT.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_TILT(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errTilt")), //

		/**
		 * ERR_WRONG_CP_PR_WIRING.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_WRONG_CP_PR_WIRING(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errWrongCpPrWiring")), //

		/**
		 * ERR_TYPE2_OVERLOAD_THR_2.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_TYPE2_OVERLOAD_THR_2(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errType2OverloadThr2")), //

		/**
		 * ERR_ACTUATOR_UNLOCKED_WHILE_CHARGING.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_ACTUATOR_UNLOCKED_WHILE_CHARGING(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errActuatorUnlockedWhileCharging")), //

		/**
		 * ERR_TILT_PREVENT_CHARGING_UNTIL_REBOOT.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_TILT_PREVENT_CHARGING_UNTIL_REBOOT(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errTiltPreventChargingUntilReboot")), //

		/**
		 * ERR_PIC24.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_PIC24(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errPic24")), //

		/**
		 * ERR_USB_STICK_HANDLING.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_USB_STICK_HANDLING(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errUsbStickHandling")), //

		/**
		 * ERR_INCORRECT_PHASE_INSTALLATION.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_INCORRECT_PHASE_INSTALLATION(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errIncorrectPhaseInstallation")), //

		/**
		 * ERR_NO_POWER.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: State
		 * </ul>
		 */
		ERR_NO_POWER(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errNoPower")), //

		/**
		 * VEHICLE STATE.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: VehicleState
		 * <li>Unit: None
		 * </ul>
		 */
		VEHICLE_STATE(Doc.of(VehicleState.values())//
				.initialValue(VehicleState.UNDEFINED)), //

		/**
		 * CP AVAILABILITY.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Boolean
		 * <li>Unit: None
		 * </ul>
		 */
		CP_AVAILABILITY(Doc.of(OpenemsType.BOOLEAN)//
				.unit(Unit.NONE)), //

		/**
		 * SAFE_CURRENT.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Boolean
		 * <li>Unit: None
		 * </ul>
		 */
		SAFE_CURRENT(Doc.of(OpenemsType.FLOAT)//
				.unit(Unit.AMPERE)), //

		/**
		 * MAX CURRENT EV.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: Ampere
		 * </ul>
		 */
		MAX_CURRENT_EV(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //

		/**
		 * MIN CURRENT LIMIT.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: Ampere
		 * </ul>
		 */
		MIN_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //

		/**
		 * CHARGE DURATION.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: SECONDS
		 * </ul>
		 */
		CHARGE_DURATION(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //

		/**
		 * EMS CURRENT LIMIT.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: AMPERE
		 * </ul>
		 */
		EMS_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY)), //
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
	 * Gets the Channel for {@link ChannelId#APPLY_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getApplyCurrentLimitChannel() {
		return this.channel(ChannelId.APPLY_CURRENT_LIMIT);
	}

	/**
	 * Sets the charge current limit of the EVCS in [A] on
	 * {@link ChannelId#APPLY_CURRENT_LIMIT} Channel.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setApplyCurrentLimit(Integer value) throws OpenemsNamedException {
		this.getApplyCurrentLimitChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#FIRMWARE_VERSION}.
	 * 
	 * @return returns the channel
	 *
	 */
	public default StringReadChannel getFirmwareVersionChannel() {
		return this.channel(ChannelId.FIRMWARE_VERSION);
	}

	/**
	 * Internal method to get the 'nextValue' of {@link ChannelId#FIRMWARE_VERSION}
	 * Channel.
	 *
	 * @return value of firmware version value
	 */
	public default Value<String> getFirmwareVersionValue() {
		return this.getFirmwareVersionChannel().getNextValue();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#FIRMWARE_VERSION}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setFirmwareVersion(String value) {
		this.getFirmwareVersionChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RAW_FIRMWARE_VERSION}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getFirmwareOutdatedChannel() {
		return this.channel(ChannelId.FIRMWARE_OUTDATED);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OCPP_CP_STATUS}.
	 *
	 * @return the Channel
	 */
	public default Channel<MennekesOcppState> getOcppCpStatusChannel() {
		return this.channel(ChannelId.OCPP_CP_STATUS);
	}

	/**
	 * Gets the OCPP Status of the EVCS charging station. See
	 * {@link ChannelId#OCPP_CP_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default MennekesOcppState getOcppCpStatus() {
		return this.getOcppCpStatusChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CP_AVAILABILITY}.
	 *
	 * @return the Channel
	 */

	public default BooleanReadChannel getCpAvailabilityChannel() {
		return this.channel(ChannelId.CP_AVAILABILITY);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_CURRENT_EV}.
	 *
	 * @return the Channel
	 */

	public default IntegerReadChannel getMaxCurrentEvChannel() {
		return this.channel(ChannelId.MAX_CURRENT_EV);
	}

	/**
	 * Gets the Channel for {@link ChannelId#VEHICLE_STATE}.
	 *
	 * @return the Channel
	 */

	public default Channel<VehicleState> getVehicleStateChannel() {
		return this.channel(ChannelId.VEHICLE_STATE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MIN_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */

	public default IntegerReadChannel getMinCurrentLimitChannel() {
		return this.channel(ChannelId.MIN_CURRENT_LIMIT);
	}

	/**
	 * Gets the Minimum current limit in [A] of the AC charger. See
	 * {@link ChannelId#MIN_CURRENT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */

	public default Value<Integer> getMinCurrentLimit() {
		return this.getMinCurrentLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#EMS_CURRENT_LIMIT}
	 * Channel. Sets the value in Ampere as an Integer-Value.
	 *
	 * @return returns the EMS current limit
	 */

	public default Value<Integer> getEmsCurrentLimit() {
		return this.getEmsCurrentLimitChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#EMS_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEmsCurrentLimitChannel() {
		return this.channel(ChannelId.EMS_CURRENT_LIMIT);
	}

}
