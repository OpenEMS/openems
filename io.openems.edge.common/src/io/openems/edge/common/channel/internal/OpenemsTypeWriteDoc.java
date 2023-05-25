package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;

public abstract class OpenemsTypeWriteDoc<T> extends OpenemsTypeDoc<T> {

	protected OpenemsTypeWriteDoc(OpenemsType type) {
		super(type);
	}

}
