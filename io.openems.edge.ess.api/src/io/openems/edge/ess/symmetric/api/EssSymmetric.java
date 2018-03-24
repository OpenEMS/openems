package io.openems.edge.ess.symmetric.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Unit;
import io.openems.edge.ess.symmetric.readonly.api.EssSymmetricReadonly;

@ProviderType
public interface EssSymmetric extends EssSymmetricReadonly {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelDoc {
		SYMMETRIC_POWER(Unit.NONE);

		private final Unit unit;

		private ChannelId(Unit unit) {
			this.unit = unit;
		}

		@Override
		public Unit getUnit() {
			return this.unit;
		}
	}

	default Channel getPower() {
		return this.getChannel(ChannelId.SYMMETRIC_POWER);
	}
}
