package io.openems.edge.evse.chargepoint.bender;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.channel.Unit.NONE;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface EvseChargePointBender extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		FIRMWARE_VERSION(Doc.of(STRING)//
				.unit(NONE)//
				.persistencePriority(HIGH) //
				.text("Readable Firmware Version")),
		FIRMWARE_OUTDATED(Doc.of(Level.INFO)//
				.translationKey(EvseChargePointBender.class, "firmwareOutdated")),
		OCPP_CP_STATUS(Doc.of(OcppState.values())//
				.persistencePriority(HIGH)), //
		ERR_RCMB_TRIGGERED(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errRcmbTriggered")), //
		ERR_VEHICLE_STATE_E(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errVehicleStateE")), //
		ERR_MODE3_DIODE_CHECK(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errMode3DiodeCheck")), //
		ERR_MCB_TYPE2_TRIGGERED(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errMcbType2Triggered")), //
		ERR_MCB_SCHUKO_TRIGGERED(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errMcbSchukoTriggered")), //
		ERR_RCD_TRIGGERED(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errRcdTriggered")), //
		ERR_CONTACTOR_WELD(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errContactorWeld")), //
		ERR_BACKEND_DISCONNECTED(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errBackendDisconnected")), //
		ERR_ACTUATOR_LOCKING_FAILED(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errActuatorLockingFailed")), //
		ERR_ACTUATOR_LOCKING_WITHOUT_PLUG_FAILED(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errActuatorLockingWithoutPlugFailed")), //
		ERR_ACTUATOR_STUCK(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errActuatorStuck")), //
		ERR_ACTUATOR_DETECTION_FAILED(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errActuatorDetectionFailed")), //
		ERR_FW_UPDATE_RUNNING(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errFwUpdateRunning")), //
		ERR_TILT(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errTilt")), //
		ERR_WRONG_CP_PR_WIRING(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errWrongCpPrWiring")), //
		ERR_TYPE2_OVERLOAD_THR_2(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errType2OverloadThr2")), //
		ERR_ACTUATOR_UNLOCKED_WHILE_CHARGING(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errActuatorUnlockedWhileCharging")), //
		ERR_TILT_PREVENT_CHARGING_UNTIL_REBOOT(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errTiltPreventChargingUntilReboot")), //
		ERR_PIC24(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errPic24")), //
		ERR_USB_STICK_HANDLING(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errUsbStickHandling")), //
		ERR_INCORRECT_PHASE_INSTALLATION(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errIncorrectPhaseInstallation")), //
		ERR_NO_POWER(Doc.of(Level.WARNING)//
				.translationKey(EvseChargePointBender.class, "errNoPower")), //
		VEHICLE_STATE(Doc.of(VehicleState.values())//
				.initialValue(VehicleState.UNDEFINED) //
				.persistencePriority(HIGH)), //
		SAFE_CURRENT(Doc.of(FLOAT)//
				.unit(AMPERE)), //
		MAX_CURRENT_EV(Doc.of(INTEGER)//
				.unit(AMPERE)), //
		MIN_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(AMPERE)), //
		CHARGE_DURATION(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		SOFTWARE_VERSION_MAJOR(Doc.of(INTEGER)), //
		SOFTWARE_VERSION_MINOR(Doc.of(INTEGER)), //
		SOFTWARE_VERSION_PATCH(Doc.of(INTEGER)), //
		SOFTWARE_VERSION_BUILD(Doc.of(INTEGER)), //
		MAX_CURRENT(Doc.of(INTEGER)//
				.persistencePriority(HIGH)), // , //
		MIN_CURRENT(Doc.of(INTEGER)//
				.persistencePriority(HIGH)), // , //
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
	 * Gets the Channel for {@link ChannelId#READABLE_FIRMWARE_VERSION}.
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
	public default EnumReadChannel getOcppStatusChannel() {
		return this.channel(ChannelId.OCPP_CP_STATUS);
	}

	/**
	 * Gets the Value for {@link ChannelId#OCPP_CP_STATUS} as {@link OcppState}.
	 * 
	 * @return the Value as {@link OcppState}
	 */
	public default OcppState getOcppStatus() {
		return this.getOcppStatusChannel().getNextValue().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#OCPP_CP_STATUS}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getVehicleStatusChannel() {
		return this.channel(ChannelId.VEHICLE_STATE);
	}

	/**
	 * Gets the Value for {@link ChannelId#OCPP_CP_STATUS} as {@link OcppState}.
	 * 
	 * @return the Value as {@link OcppState}
	 */
	public default VehicleState getVehicleState() {
		return this.getVehicleStatusChannel().getNextValue().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxCurrentChannel() {
		return this.channel(ChannelId.MAX_CURRENT);
	}

	/**
	 * Gets the Value for {@link ChannelId#MAX_CURRENT}.
	 * 
	 * @return the Value
	 */
	public default Integer getMaxCurrent() {
		return this.getMaxCurrentChannel().getNextValue().orElse(null);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MIN_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMinCurrentChannel() {
		return this.channel(ChannelId.MIN_CURRENT);
	}

	/**
	 * Gets the Value for {@link ChannelId#MIN_CURRENT}.
	 * 
	 * @return the Value
	 */
	public default Integer getMinCurrent() {
		return this.getMinCurrentChannel().getNextValue().orElse(null);
	}

}
