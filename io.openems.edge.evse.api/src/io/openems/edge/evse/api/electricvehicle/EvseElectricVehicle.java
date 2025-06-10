package io.openems.edge.evse.api.electricvehicle;

import com.google.common.collect.ImmutableList;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.Limit;

public interface EvseElectricVehicle extends OpenemsComponent {

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

	public static record ChargeParams(ImmutableList<Limit> limits, ImmutableList<Profile> profiles) {
	}

	/**
	 * Gets the {@link ChargeParams}s.
	 * 
	 * @return list of {@link ChargeParams}s
	 */
	public ChargeParams getChargeParams();
}
