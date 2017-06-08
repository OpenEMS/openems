package io.openems.impl.device.simulator;

public class RandomLoadGenerator implements LoadGenerator {

	private long min;
	private long max;

	public long getMin() {
		return min;
	}

	public void setMin(long min) {
		this.min = min;
	}

	public long getMax() {
		return max;
	}

	public void setMax(long max) {
		this.max = max;
	}

	public RandomLoadGenerator(long min, long max) {
		super();
		this.min = min;
		this.max = max;
	}

	@Override
	public long getLoad() {
		return min + (int) (Math.random() * ((max - min) + 1));
	}

}
