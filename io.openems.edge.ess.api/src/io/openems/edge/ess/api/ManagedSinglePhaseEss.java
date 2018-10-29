package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.doc.Doc;

@ProviderType
public interface ManagedSinglePhaseEss extends ManagedSymmetricEss, SinglePhaseEss {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}
}
