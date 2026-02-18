package io.openems.edge.common.channel.dynamicdoctext;

import io.openems.common.session.Language;
import io.openems.edge.common.channel.ChannelId;

class DefaultChannelParameterProviderImpl<V> extends ChannelParameterProvider<V> {
	protected DefaultChannelParameterProviderImpl(ChannelId channelId) {
		super(channelId);
	}

	@Override
	public String getText(Language lang) {
		return this.getChannelValueAsString();
	}

	@Override
	public ParameterProvider clone() {
		return new DefaultChannelParameterProviderImpl<>(this.channelId);
	}
}