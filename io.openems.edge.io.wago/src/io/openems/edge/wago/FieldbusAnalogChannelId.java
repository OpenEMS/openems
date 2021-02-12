package io.openems.edge.wago;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;

public class FieldbusAnalogChannelId implements io.openems.edge.common.channel.ChannelId {

	private final String name;
	private final OpenemsTypeDoc<String> doc;

	public FieldbusAnalogChannelId(String name, OpenemsTypeDoc<String> doc) {
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
