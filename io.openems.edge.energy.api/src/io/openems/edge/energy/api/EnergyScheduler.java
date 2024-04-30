package io.openems.edge.energy.api;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;

/**
 * The global Energy Schedule optimizer singleton.
 */
public interface EnergyScheduler extends OpenemsComponent, ComponentJsonApi {

	public static final String SINGLETON_SERVICE_PID = "Core.Energy";
	public static final String SINGLETON_COMPONENT_ID = "_energy";

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
