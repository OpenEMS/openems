package io.openems.edge.meter.algo2.algotypes;


import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;

import java.nio.ByteBuffer;

/**
 * A SignedDoublewordElement represents a Long value in an
 * {@link AbstractDoubleWordElement}.
 */
public class Algo3Bytes extends AbstractAlgo3Bytes<Algo3Bytes, Long> {

	public Algo3Bytes(int address) {
		super(OpenemsType.LONG, address);
	}

	@Override
	protected Algo3Bytes self() {
		return this;
	}

	protected Long fromByteBuffer(ByteBuffer buff) {
		return Long.valueOf(buff.getLong());
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Long value) {
		return buff.putLong(value.longValue());
	}

}
