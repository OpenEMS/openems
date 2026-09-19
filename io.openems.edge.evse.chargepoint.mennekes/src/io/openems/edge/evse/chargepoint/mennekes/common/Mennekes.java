package io.openems.edge.evse.chargepoint.mennekes.common;

import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.chargepoint.mennekes.enums.PhaseSwitchMode;

/**
 * Mennekes Amtron Professional charging protocol interface.
 * 
 * <p>
 * Defines the interface for Mennekes Amtron Professional
 */
public interface Mennekes extends OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Apply charge current limit.
		 * 
		 * <p>
		 * WriteChannel for the modbus register to apply the charge power given by the
		 * applyChargePowerLimit method
		 */
		SET_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(AMPERE)//
				.accessMode(WRITE_ONLY)), //

		SET_POWER_LIMIT(Doc.of(INTEGER)//
				.unit(WATT)//
				.accessMode(WRITE_ONLY)), //

		EMS_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(AMPERE)), //

		HEMS_MIN_POWER(Doc.of(INTEGER)//
				.unit(WATT)), //

		HEMS_MAX_POWER(Doc.of(INTEGER)//
				.unit(WATT)), //
		PHASE_SWITCH_MODE(Doc.of(PhaseSwitchMode.values())//
				.persistencePriority(PersistencePriority.HIGH)),

		PHASE_SWITCH_PAUSE(Doc.of(INTEGER)//
				.unit(Unit.SECONDS)),

		PHASE_SWITCH_RUNNING(Doc.of(BOOLEAN)), //

		DEVICE_ID(Doc.of(DeviceID.values())), //

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
	 * Gets the Channel for {@link ChannelId#SET_POWER_LIMIT}. Used for EVSE.
	 *
	 * @return the Channel
	 */
	default IntegerWriteChannel getApplyPowerLimitChannel() {
		return this.channel(ChannelId.SET_POWER_LIMIT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CURRENT_LIMIT}. Used for EVCS.
	 *
	 * @return the Channel
	 */
	default IntegerWriteChannel getApplyCurrentLimitChannel() {
		return this.channel(ChannelId.SET_CURRENT_LIMIT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_SWITCH_MODE}.
	 * 
	 * @return the Channel
	 */
	default EnumReadChannel getPhaseSwitchModeChannel() {
		return this.channel(ChannelId.PHASE_SWITCH_MODE);
	}

	/**
	 * Gets the {@link PhaseSwitchMode}.
	 * 
	 * @return the {@link PhaseSwitchMode}
	 */
	default PhaseSwitchMode getPhaseSwitchMode() {
		return this.getPhaseSwitchModeChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_SWITCH_RUNNING}.
	 *
	 * @return the Channel
	 */
	default BooleanReadChannel getPhaseSwitchRunningChannel() {
		return this.channel(ChannelId.PHASE_SWITCH_RUNNING);
	}

	/**
	 * Returns whether the internal phase switch is currently running.
	 *
	 * @return true while the phase switch is running
	 */
	default boolean isPhaseSwitchRunning() {
		return this.getPhaseSwitchRunningChannel().value().orElse(false);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEVICE_ID}.
	 *
	 * @return the Channel
	 */
	default EnumReadChannel getMennekesDeviceIdChannel() {
		return this.channel(ChannelId.DEVICE_ID);
	}

	/**
	 * Gets the {@link DeviceID}.
	 *
	 * @return the {@link DeviceID}
	 */
	default DeviceID getMennekesDeviceId() {
		return this.getMennekesDeviceIdChannel().value().asEnum();
	}

}
