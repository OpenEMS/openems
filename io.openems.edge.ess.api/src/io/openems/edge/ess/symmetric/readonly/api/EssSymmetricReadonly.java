package io.openems.edge.ess.symmetric.readonly.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.ess.api.Ess;

@ProviderType
public interface EssSymmetricReadonly extends Ess {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		ACTIVE_POWER(new Doc().unit(Unit.W)), //
		REACTIVE_POWER(new Doc().unit(Unit.VAR));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	default Channel getActivePower() {
		return this.channel(ChannelId.ACTIVE_POWER);
	}

	default Channel getReactivePower() {
		return this.channel(ChannelId.REACTIVE_POWER);
	}
}
