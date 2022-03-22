package io.openems.edge.bosch.bpts5hybrid.core;

import java.util.Optional;

import io.openems.common.channel.Level;
import io.openems.edge.bosch.bpts5hybrid.ess.BoschBpts5HybridEss;
import io.openems.edge.bosch.bpts5hybrid.meter.BoschBpts5HybridMeter;
import io.openems.edge.bosch.bpts5hybrid.pv.BoschBpts5HybridPv;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface BoschBpts5HybridCore extends OpenemsComponent {

	public void setEss(BoschBpts5HybridEss ess);

	public void setPv(BoschBpts5HybridPv boschBpts5HybridPv);

	public void setMeter(BoschBpts5HybridMeter boschBpts5HybridMeter);

	public Optional<BoschBpts5HybridEss> getEss();

	public Optional<BoschBpts5HybridPv> getPv();

	public Optional<BoschBpts5HybridMeter> getMeter();

	public enum CoreChannelId implements io.openems.edge.common.channel.ChannelId {
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT));

		private final Doc doc;

		private CoreChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getSlaveCommunicationFailedChannel() {
		return this.channel(CoreChannelId.SLAVE_COMMUNICATION_FAILED);
	}

	/**
	 * Gets the Slave Communication Failed State. See
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getSlaveCommunicationFailed() {
		return this.getSlaveCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSlaveCommunicationFailed(boolean value) {
		this.getSlaveCommunicationFailedChannel().setNextValue(value);
	}
}
