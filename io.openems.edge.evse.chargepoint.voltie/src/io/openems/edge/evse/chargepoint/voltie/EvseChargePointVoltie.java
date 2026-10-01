package io.openems.edge.evse.chargepoint.voltie;

import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.chargepoint.voltie.enums.ChargeStopReason;
import io.openems.edge.evse.chargepoint.voltie.enums.EvseState;

/**
 * Channels of the Voltie AC charge-point.
 *
 * <p>
 * Register map source: Voltie Modbus API documentation v1.4 (2026-09-23),
 * firmware build 370 and later.
 */
public interface EvseChargePointVoltie extends OpenemsComponent {

	/**
	 * Bit 0 of the capability bitmask (register 0x0019): 1&lt;-&gt;3 phase
	 * switching is supported, that is the forced single-phase register 0x0016 is
	 * writable on this hardware.
	 */
	public static final int CAPABILITY_PHASE_SWITCHING = 0x0001;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/** Voltie charger ID (register 0x0000). */
		CHARGER_ID(Doc.of(INTEGER) //
				.persistencePriority(PersistencePriority.LOW)), //
		/** EVSE firmware build number, e.g. 370 (register 0x0001). */
		FIRMWARE_BUILD(Doc.of(INTEGER) //
				.persistencePriority(PersistencePriority.LOW)), //
		/** MCU serial number (registers 0x0002..0x0005, LSW first). */
		MCU_SERIAL(Doc.of(LONG) //
				.persistencePriority(PersistencePriority.LOW)), //
		/** Power board serial number (registers 0x0006..0x0009, LSW first). */
		POWER_BOARD_SERIAL(Doc.of(LONG) //
				.persistencePriority(PersistencePriority.LOW)), //
		/** EVSE state (register 0x000A). */
		EVSE_STATE(Doc.of(EvseState.values()) //
				.persistencePriority(PersistencePriority.LOW)), //
		/** Autostart enabled (register 0x000B). Never written by this driver. */
		AUTOSTART_ENABLED(Doc.of(BOOLEAN) //
				.persistencePriority(PersistencePriority.LOW)), //
		/** Charging enabled (register 0x000C). */
		CHARGING_ENABLED(Doc.of(BOOLEAN) //
				.persistencePriority(PersistencePriority.LOW)), //
		/** Charging active right now; the authoritative flag (register 0x000D). */
		CHARGING_ACTIVE(Doc.of(BOOLEAN) //
				.persistencePriority(PersistencePriority.LOW)), //
		/**
		 * Number of phases with mains voltage present on the charger INPUT (register
		 * 0x000E).
		 *
		 * <p>
		 * This is supply-side information only. It stays 3 in forced single-phase
		 * mode; the phases the EV actually charges with are reported separately, see
		 * {@link ChannelId#PHASES_IN_USE}.
		 */
		MAINS_PHASES(Doc.of(INTEGER) //
				.persistencePriority(PersistencePriority.LOW)), //
		/** Stored DLM mode (register 0x000F). Never written by this driver. */
		STORED_DLM_MODE(Doc.of(INTEGER) //
				.persistencePriority(PersistencePriority.LOW)), //
		/** Charge stop reason (register 0x0012). */
		CHARGE_STOP_REASON(Doc.of(ChargeStopReason.values()) //
				.persistencePriority(PersistencePriority.LOW)), //
		/**
		 * Autonomous current limit in [mA] (register 0x0013). Never written by this
		 * driver.
		 */
		AUTONOMOUS_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(MILLIAMPERE) //
				.persistencePriority(PersistencePriority.LOW)), //
		/** Software current limit read-back in [mA] (register 0x0014). */
		CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(MILLIAMPERE) //
				.persistencePriority(PersistencePriority.LOW)), //
		/** Effective DLM mode (register 0x0015). */
		EFFECTIVE_DLM_MODE(Doc.of(INTEGER) //
				.persistencePriority(PersistencePriority.LOW)), //
		/**
		 * Forced single-phase state (register 0x0016): false=three-phase,
		 * true=single-phase. The setting survives restarts.
		 */
		FORCED_SINGLE_PHASE(Doc.of(BOOLEAN) //
				.persistencePriority(PersistencePriority.LOW)), //
		/**
		 * Communication-loss watchdog in [s] (register 0x0017): if the Modbus master
		 * stays silent this long, the allowed current drops to 0 A; 0 or 255 =
		 * disabled.
		 *
		 * <p>
		 * The register is writable from firmware build 357 (range 1..254 s), but this
		 * driver only reads it: writing would silently overwrite a setting the user
		 * made in the Voltie app.
		 */
		COMM_WATCHDOG_TIMEOUT(Doc.of(INTEGER)//
				.unit(SECONDS) //
				.persistencePriority(PersistencePriority.LOW)), //
		/**
		 * Hardware maximum current capacity in [mA] (register 0x0018;
		 * EVSE-board potentiometer limit, cable limit not included).
		 */
		HARDWARE_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(MILLIAMPERE) //
				.persistencePriority(PersistencePriority.LOW)), //
		/**
		 * Capability bitmask (register 0x0019).
		 *
		 * <p>
		 * Bit 0 ({@link EvseChargePointVoltie#CAPABILITY_PHASE_SWITCHING}) reports
		 * that 1&lt;-&gt;3 phase switching is supported, that is register 0x0016 is
		 * writable on this hardware. All other bits are reserved and read 0.
		 */
		CAPABILITY_FLAGS(Doc.of(INTEGER) //
				.persistencePriority(PersistencePriority.LOW)), //
		/**
		 * Number of phases the EV is actually charging with, 1..3 (register 0x001A);
		 * 0 = unknown or no charging in progress.
		 *
		 * <p>
		 * Reported by the firmware, which counts a phase as loaded from 1.0 A. Unlike
		 * {@link ChannelId#MAINS_PHASES} this reflects the phases under load, so it
		 * reads 1 in forced single-phase mode on a three-phase supply.
		 */
		PHASES_IN_USE(Doc.of(INTEGER) //
				.persistencePriority(PersistencePriority.LOW)), //

		/** Charge duration of the running session in [s] (register 0x200C). */
		CHARGE_DURATION(Doc.of(INTEGER)//
				.unit(SECONDS) //
				.persistencePriority(PersistencePriority.LOW)), //
		/**
		 * Session energy in [Wh], converted from [Ws] (register 0x200E).
		 *
		 * <p>
		 * Keeps the last session's value after session end; it is cleared only when
		 * the next session starts. Use {@link ChannelId#CHARGING_ACTIVE} to detect
		 * a running session.
		 */
		ENERGY_SESSION(Doc.of(INTEGER)//
				.unit(WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)), //
		/**
		 * Instantaneous available current capacity in [mA] (register 0x2012;
		 * cable-aware dynamic maximum).
		 */
		AVAILABLE_CURRENT_CAPACITY(Doc.of(INTEGER)//
				.unit(MILLIAMPERE) //
				.persistencePriority(PersistencePriority.LOW)), //

		DEBUG_SET_CHARGING_ENABLED(Doc.of(INTEGER)), //
		/** Write 1=start/enable charging, 0=stop (FC6 to register 0x000C). */
		SET_CHARGING_ENABLED(Doc.of(INTEGER)//
				.accessMode(WRITE_ONLY)//
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_CHARGING_ENABLED)), //
		DEBUG_SET_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(MILLIAMPERE)), //
		/**
		 * Write the software current limit in [mA], valid 6000..32000 (FC6 to register
		 * 0x0014). The device resolution is 1 A: a mA remainder is truncated by the
		 * firmware, so only full-Ampere multiples are ever written.
		 */
		SET_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(MILLIAMPERE)//
				.accessMode(WRITE_ONLY)//
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_CURRENT_LIMIT)), //
		DEBUG_SET_FORCED_SINGLE_PHASE(Doc.of(INTEGER)), //
		/**
		 * Write 0=three-phase, 1=single-phase (FC6 to register 0x0016). Rejected with
		 * exception 0x03 while a charging session is active, on hardware that does not
		 * report {@link EvseChargePointVoltie#CAPABILITY_PHASE_SWITCHING}, when
		 * control-by-Modbus is disabled, or on relay/EEPROM error.
		 */
		SET_FORCED_SINGLE_PHASE(Doc.of(INTEGER)//
				.accessMode(WRITE_ONLY)//
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_FORCED_SINGLE_PHASE)), //

		/** The charge-point reports an internal error state. */
		EVSE_FAULT(Doc.of(Level.FAULT)//
				.text("Voltie charge-point reports an internal error state; see EvseState channel")), //
		/** Firmware build is older than 370. */
		FIRMWARE_OUTDATED(Doc.of(Level.WARNING)//
				.text("Voltie firmware build is older than 370; "
						+ "update the charger firmware, the charge-point is not read or controlled")), //
		/** The phase switch write was not accepted by the charge-point. */
		PHASE_SWITCH_FAILED(Doc.of(Level.WARNING)//
				.text("Phase switch was rejected by the Voltie charge-point "
						+ "(control-by-Modbus disabled or relay/EEPROM error); "
						+ "phase switching is disabled until the vehicle is unplugged")), //
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
	 * Gets the Channel for {@link ChannelId#EVSE_STATE}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getEvseStateChannel() {
		return this.channel(ChannelId.EVSE_STATE);
	}

	/**
	 * Gets the {@link EvseState}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default EvseState getEvseState() {
		return this.getEvseStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#FIRMWARE_BUILD}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getFirmwareBuildChannel() {
		return this.channel(ChannelId.FIRMWARE_BUILD);
	}

	/**
	 * Gets the firmware build number. See {@link ChannelId#FIRMWARE_BUILD}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getFirmwareBuild() {
		return this.getFirmwareBuildChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGING_ENABLED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getChargingEnabledChannel() {
		return this.channel(ChannelId.CHARGING_ENABLED);
	}

	/**
	 * Gets the charging-enabled state. See {@link ChannelId#CHARGING_ENABLED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getChargingEnabled() {
		return this.getChargingEnabledChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGING_ACTIVE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getChargingActiveChannel() {
		return this.channel(ChannelId.CHARGING_ACTIVE);
	}

	/**
	 * Gets the charging-active state. See {@link ChannelId#CHARGING_ACTIVE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getChargingActive() {
		return this.getChargingActiveChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#FORCED_SINGLE_PHASE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getForcedSinglePhaseChannel() {
		return this.channel(ChannelId.FORCED_SINGLE_PHASE);
	}

	/**
	 * Gets the forced single-phase state. See
	 * {@link ChannelId#FORCED_SINGLE_PHASE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getForcedSinglePhase() {
		return this.getForcedSinglePhaseChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentLimitChannel() {
		return this.channel(ChannelId.CURRENT_LIMIT);
	}

	/**
	 * Gets the software current limit read-back in [mA]. See
	 * {@link ChannelId#CURRENT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentLimit() {
		return this.getCurrentLimitChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#HARDWARE_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getHardwareCurrentLimitChannel() {
		return this.channel(ChannelId.HARDWARE_CURRENT_LIMIT);
	}

	/**
	 * Gets the hardware maximum current capacity in [mA]. See
	 * {@link ChannelId#HARDWARE_CURRENT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHardwareCurrentLimit() {
		return this.getHardwareCurrentLimitChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CAPABILITY_FLAGS}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCapabilityFlagsChannel() {
		return this.channel(ChannelId.CAPABILITY_FLAGS);
	}

	/**
	 * Gets the capability bitmask. See {@link ChannelId#CAPABILITY_FLAGS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCapabilityFlags() {
		return this.getCapabilityFlagsChannel().value();
	}

	/**
	 * Is 1&lt;-&gt;3 phase switching supported by this charge-point?.
	 *
	 * <p>
	 * Evaluates bit 0 of the capability bitmask (register 0x0019). Returns false
	 * while the register has not been read yet.
	 *
	 * @return true if register 0x0016 is writable on this hardware
	 */
	public default boolean isPhaseSwitchingSupported() {
		final var flags = this.getCapabilityFlags().get();
		return flags != null && (flags & CAPABILITY_PHASE_SWITCHING) != 0;
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CHARGING_ENABLED}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetChargingEnabledChannel() {
		return this.channel(ChannelId.SET_CHARGING_ENABLED);
	}

	/**
	 * Sets the next write value for {@link ChannelId#SET_CHARGING_ENABLED}.
	 *
	 * @param enabled true to enable charging
	 * @throws OpenemsNamedException on error
	 */
	public default void setChargingEnabled(boolean enabled) throws OpenemsNamedException {
		this.getSetChargingEnabledChannel().setNextWriteValue(enabled ? 1 : 0);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetCurrentLimitChannel() {
		return this.channel(ChannelId.SET_CURRENT_LIMIT);
	}

	/**
	 * Sets the next write value for {@link ChannelId#SET_CURRENT_LIMIT}.
	 *
	 * @param currentInMilliAmpere the current in [mA]
	 * @throws OpenemsNamedException on error
	 */
	public default void setCurrentLimit(int currentInMilliAmpere) throws OpenemsNamedException {
		this.getSetCurrentLimitChannel().setNextWriteValue(currentInMilliAmpere);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_FORCED_SINGLE_PHASE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetForcedSinglePhaseChannel() {
		return this.channel(ChannelId.SET_FORCED_SINGLE_PHASE);
	}

	/**
	 * Sets the next write value for {@link ChannelId#SET_FORCED_SINGLE_PHASE}.
	 *
	 * @param singlePhase true for single-phase, false for three-phase
	 * @throws OpenemsNamedException on error
	 */
	public default void setForcedSinglePhase(boolean singlePhase) throws OpenemsNamedException {
		this.getSetForcedSinglePhaseChannel().setNextWriteValue(singlePhase ? 1 : 0);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_SWITCH_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getPhaseSwitchFailedChannel() {
		return this.channel(ChannelId.PHASE_SWITCH_FAILED);
	}
}
