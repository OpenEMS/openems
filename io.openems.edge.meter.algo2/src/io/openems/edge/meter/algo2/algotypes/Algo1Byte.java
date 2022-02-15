package io.openems.edge.meter.algo2.algotypes;

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;

import java.nio.ByteBuffer;

/**
 * A SignedDoublewordElement represents a Long value in an
 * {@link AbstractDoubleWordElement}.
 */
public class Algo1Byte  extends AbstractAlgo1Byte<Algo1Byte , Long> {

	public Algo1Byte(int address) {
		super(OpenemsType.LONG, address);
	}

	@Override
	protected Algo1Byte  self() {
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
