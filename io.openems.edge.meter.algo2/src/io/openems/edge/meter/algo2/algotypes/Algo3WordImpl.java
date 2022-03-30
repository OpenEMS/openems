package io.openems.edge.meter.algo2.algotypes;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

public class Algo3WordImpl extends Algo3Word<Algo3WordImpl, Long> {

	public Algo3WordImpl(int address) {
		super(OpenemsType.LONG, address);
	}

	@Override
	protected Algo3WordImpl self() {
		return this;
	}

	protected Long fromByteBuffer(ByteBuffer buff) {
		return Long.valueOf(buff.getLong());
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Long value) {
		return buff.putLong(value.longValue());
	}

}
