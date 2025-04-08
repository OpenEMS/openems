package io.openems.edge.timeofusetariff.manual.octopus.heat;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface TouOctopusHeat extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		INVALID_PRICE(Doc.of(Level.WARNING) //
				.text("Unable to calculate prices due to invalid configuration")), //
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
