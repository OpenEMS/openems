package io.openems.edge.battery.soltaro.multirack;

import io.openems.edge.common.channel.doc.Doc;

public class ChannelIdImpl implements io.openems.edge.common.channel.doc.ChannelId {

	private final String name;
	private final Doc doc;

	public ChannelIdImpl(String name, Doc doc) {
		this.name = name;
		this.doc = doc;
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}
