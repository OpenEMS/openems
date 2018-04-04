package io.openems.edge.ess.symmetric.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.ess.symmetric.readonly.api.EssSymmetricReadonly;

@ProviderType
public interface EssSymmetric extends EssSymmetricReadonly {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SYMMETRIC_POWER(new Doc().unit(Unit.NONE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	default Channel getPower() {
		return this.channel(ChannelId.SYMMETRIC_POWER);
	}
}
