package io.openems.edge.wago;

import io.openems.edge.common.channel.doc.Doc;

public class FieldbusChannel implements io.openems.edge.common.channel.doc.ChannelId {

	private final String name;
	private final Doc doc = new Doc();

	public FieldbusChannel(String name) {
		this.name = name;
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
