package io.openems.edge.sonnen.meter;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.sonnen.core.SonnenPowermeter;
import io.openems.edge.sonnen.core.SonnenStatus;

public interface MeterSonnen extends ElectricityMeter, OpenemsComponent {

	/**
	 * Updates the channels from the '/api/v2/status' response. Called by
	 * {@link io.openems.edge.sonnen.core.SonnenCore}.
	 *
	 * @param status the {@link SonnenStatus}
	 */
	void updateFromStatus(SonnenStatus status);

	/**
	 * Updates the channels from the 'consumption' entry of the
	 * '/api/v2/powermeter' response. Called by
	 * {@link io.openems.edge.sonnen.core.SonnenCore}.
	 *
	 * @param powermeter the {@link SonnenPowermeter}; may be null
	 */
	void updateFromPowermeter(SonnenPowermeter powermeter);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
}
