package io.openems.impl.device.simulator;

public class FixValueLoadGenerator implements LoadGenerator {

	private long value;

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	@Override
	public long getLoad() {
		return value;
	}

}
