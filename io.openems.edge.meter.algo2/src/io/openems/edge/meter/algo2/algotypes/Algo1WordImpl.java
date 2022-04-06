package io.openems.edge.meter.algo2.algotypes;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

public class Algo1WordImpl  extends Algo1Word<Algo1Word, Long> {

	public Algo1WordImpl(int address) {
		super(OpenemsType.LONG, address);
	}

	@Override
	protected Algo1WordImpl self() {
		return this;
	}

	protected Long fromByteBuffer(ByteBuffer buff) {
		Long theVal = Long.valueOf(buff.getInt());
		super.log.debug("1 byte received buffer " + buff.toString());
		return theVal;
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Long value) {
		super.log.debug("1 byte sent to buffer " + value);
		return buff.put(value.byteValue());
	}

}
