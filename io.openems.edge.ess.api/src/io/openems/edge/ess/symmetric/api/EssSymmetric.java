package io.openems.edge.ess.symmetric.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Option;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.ess.symmetric.readonly.api.EssSymmetricReadonly;

@ProviderType
public interface EssSymmetric extends EssSymmetricReadonly {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelDoc {
		SYMMETRIC_POWER(Unit.NONE);

		private final Unit unit;

		private ChannelId(Unit unit) {
			this.unit = unit;
		}

		@Override
		public Unit getUnit() {
			return this.unit;
		}

		@Override
		public Option getOptions() {
			return null;
		}
	}

	default Channel getPower() {
		return this.channel(ChannelId.SYMMETRIC_POWER);
	}
}
