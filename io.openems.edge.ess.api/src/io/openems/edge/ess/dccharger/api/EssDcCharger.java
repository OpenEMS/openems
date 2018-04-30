package io.openems.edge.ess.dccharger.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface EssDcCharger extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Actual Power
		 * 
		 * <ul>
		 * <li>Interface: Ess DC Charger
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive
		 * </ul>
		 */
		DC_ACTUAL_POWER(new Doc().unit(Unit.WATT)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Actual Power
	 * 
	 * @see EssDcCharger.ChannelId#DC_ACTUAL_POWER
	 * 
	 * @return
	 */
	default Channel<Integer> getActualPower() {
		return this.channel(ChannelId.DC_ACTUAL_POWER);
	}
}
