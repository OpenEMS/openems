package io.openems.edge.evse.chargepoint.hardybarth;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.chargepoint.hardybarth.common.HardyBarth;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvseChargePointHardyBarth extends OpenemsComponent, HardyBarth, EvseChargePoint, ElectricityMeter {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATUS(Doc.of(ChargePointStatus.values())) //
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
	 * Gets the Channel for {@link ChannelId#STATUS}.
	 *
	 * @return the Channel
	 */
	public default Channel<ChargePointStatus> getChargePointStatusChannel() {
		return this.channel(ChannelId.STATUS);
	}

	/**
	 * Gets the {@link ChargePointStatus}. See {@link ChannelId#STATUS}.
	 *
	 * @return the Channel value
	 */
	public default ChargePointStatus getChargePointStatus() {
		return this.getChargePointStatusChannel().value().asEnum();
	}
}
