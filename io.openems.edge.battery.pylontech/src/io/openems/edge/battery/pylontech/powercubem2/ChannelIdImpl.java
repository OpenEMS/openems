package io.openems.edge.battery.pylontech.powercubem2;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;

public class ChannelIdImpl implements ChannelId {

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