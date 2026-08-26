package io.openems.edge.evse.api.electricvehicle;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities;

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

	/**
	 * Gets the {@link ElectricVehicleAbilities}.
	 * 
	 * @return {@link ElectricVehicleAbilities}
	 */
	public ElectricVehicleAbilities getElectricVehicleAbilities();
}
