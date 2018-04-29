package io.openems.edge.meter.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface Meter extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Frequency [mHz]
		 */
		FREQUENCY(new Doc().unit(Unit.MILLIHERTZ)) //

		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the type of this Meter
	 * 
	 * @return
	 */
	MeterType getMeterType();

}
