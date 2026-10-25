package io.openems.edge.sonnen.ess;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.sonnen.core.SonnenStatus;

public interface EssSonnen extends ManagedSymmetricEss, OpenemsComponent {

	/**
	 * Updates the channels from the '/api/v2/status' response. Called by
	 * {@link io.openems.edge.sonnen.core.SonnenCore}.
	 *
	 * @param status the {@link SonnenStatus}
	 */
	void updateFromStatus(SonnenStatus status);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Operating mode that is currently set on the sonnenBatterie.
		 *
		 * <ul>
		 * <li>Type: Enum {@link io.openems.edge.sonnen.core.OperatingMode}
		 * </ul>
		 */
		OPERATING_MODE(Doc.of(io.openems.edge.sonnen.core.OperatingMode.values()) //
				.persistencePriority(io.openems.common.channel.PersistencePriority.MEDIUM)), //

		/**
		 * The sonnenBatterie currently does not allow discharging, e.g. due to
		 * battery maintenance.
		 */
		DISCHARGE_NOT_ALLOWED(Doc.of(Level.WARNING)), //

		/**
		 * The configured {@link ControlMode} is SMART, but the global PID filter
		 * is enabled. SMART mode is not working with PID filter.
		 */
		SMART_MODE_NOT_WORKING_WITH_FILTER(Doc.of(Level.WARNING));

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
	 * Gets the Channel for {@link ChannelId#OPERATING_MODE}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getOperatingModeChannel() {
		return this.channel(ChannelId.OPERATING_MODE);
	}

	/**
	 * Gets the current {@link io.openems.edge.sonnen.core.OperatingMode}. See
	 * {@link ChannelId#OPERATING_MODE}.
	 *
	 * @return the {@link io.openems.edge.sonnen.core.OperatingMode}
	 */
	public default io.openems.edge.sonnen.core.OperatingMode getOperatingMode() {
		return this.getOperatingModeChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#DISCHARGE_NOT_ALLOWED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getDischargeNotAllowedChannel() {
		return this.channel(ChannelId.DISCHARGE_NOT_ALLOWED);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DISCHARGE_NOT_ALLOWED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDischargeNotAllowed(boolean value) {
		this.getDischargeNotAllowedChannel().setNextValue(value);
	}
}
