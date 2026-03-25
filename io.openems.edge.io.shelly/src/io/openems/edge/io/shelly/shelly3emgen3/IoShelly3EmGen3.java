package io.openems.edge.io.shelly.shelly3emgen3;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.io.shelly.common.component.ShellyEnergyMeter;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBase;

public interface IoShelly3EmGen3 extends IoGen2ShellyBase, ShellyEnergyMeter {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		//
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
