package io.openems.edge.io.hal.raspberrypi;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.hal.modberry.RaspberryPiPlattform;

public interface RaspberryPiInterface extends OpenemsComponent {

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
	
	<T extends RaspberryPiPlattform> T getHardwareAs(Class<T> clazz);

}
