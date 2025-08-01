package io.openems.edge.evcs.mennekes;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.channel.Unit.NONE;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
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
		 * Apply charge current limit.
		 * 
		 * <p>
		 * WriteChannel for the modbus register to apply the charge power given by the
		 * applyChargePowerLimit method
		 */
		APPLY_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(AMPERE)//
				.accessMode(READ_WRITE)), //

		FIRMWARE_VERSION(Doc.of(STRING)//
				.unit(NONE)//
				.text("Readable Firmware Version")),
		FIRMWARE_OUTDATED(Doc.of(Level.INFO)//
				.translationKey(EvcsMennekes.class, "firmwareOutdated")),
		RAW_FIRMWARE_VERSION(Doc.of(INTEGER)//
				.unit(NONE)//
				.text("Firmware Version as Long; has to converted to hex to interpret")), //
		OCPP_CP_STATUS(Doc.of(MennekesOcppState.values())//
				.persistencePriority(HIGH)), //
		ERR_RCMB_TRIGGERED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errRcmbTriggered")), //
		ERR_VEHICLE_STATE_E(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errVehicleStateE")), //
		ERR_MODE3_DIODE_CHECK(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errMode3DiodeCheck")), //
		ERR_MCB_TYPE2_TRIGGERED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errMcbType2Triggered")), //
		ERR_MCB_SCHUKO_TRIGGERED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errMcbSchukoTriggered")), //
		ERR_RCD_TRIGGERED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errRcdTriggered")), //
		ERR_CONTACTOR_WELD(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errContactorWeld")), //
		ERR_BACKEND_DISCONNECTED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errBackendDisconnected")), //
		ERR_ACTUATOR_LOCKING_FAILED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errActuatorLockingFailed")), //
		ERR_ACTUATOR_LOCKING_WITHOUT_PLUG_FAILED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errActuatorLockingWithoutPlugFailed")), //
		ERR_ACTUATOR_STUCK(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errActuatorStuck")), //
		ERR_ACTUATOR_DETECTION_FAILED(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errActuatorDetectionFailed")), //
		ERR_FW_UPDATE_RUNNING(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errFwUpdateRunning")), //
		ERR_TILT(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errTilt")), //
		ERR_WRONG_CP_PR_WIRING(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errWrongCpPrWiring")), //
		ERR_TYPE2_OVERLOAD_THR_2(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errType2OverloadThr2")), //
		ERR_ACTUATOR_UNLOCKED_WHILE_CHARGING(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errActuatorUnlockedWhileCharging")), //
		ERR_TILT_PREVENT_CHARGING_UNTIL_REBOOT(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errTiltPreventChargingUntilReboot")), //
		ERR_PIC24(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errPic24")), //
		ERR_USB_STICK_HANDLING(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errUsbStickHandling")), //
		ERR_INCORRECT_PHASE_INSTALLATION(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errIncorrectPhaseInstallation")), //
		ERR_NO_POWER(Doc.of(Level.WARNING)//
				.translationKey(EvcsMennekes.class, "errNoPower")), //
		VEHICLE_STATE(Doc.of(VehicleState.values())//
				.initialValue(VehicleState.UNDEFINED)), //
		SAFE_CURRENT(Doc.of(FLOAT)//
				.unit(AMPERE)), //
		MAX_CURRENT_EV(Doc.of(INTEGER)//
				.unit(AMPERE)), //
		MIN_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(AMPERE)), //
		CHARGE_DURATION(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		EMS_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(AMPERE)//
				.accessMode(READ_ONLY)), //
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
	 * Gets the Channel for {@link ChannelId#RAW_FIRMWARE_VERSION}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getFirmwareOutdatedChannel() {
		return this.channel(ChannelId.FIRMWARE_OUTDATED);
	}
}
