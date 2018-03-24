package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Unit;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface Ess extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelDoc {
		SOC(Unit.PERCENT);

		private final Unit unit;

		private ChannelId(Unit unit) {
			this.unit = unit;
		}

		@Override
		public Unit getUnit() {
			return this.unit;
		}
	}

	default Channel getSoc() {
		return this.getChannel(ChannelId.SOC);
	}
}
