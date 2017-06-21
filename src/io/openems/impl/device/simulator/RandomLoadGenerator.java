package io.openems.impl.device.simulator;

import com.google.gson.JsonObject;

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

	public RandomLoadGenerator(JsonObject config) {
		super();
		this.min = config.get("min").getAsLong();
		this.max = config.get("max").getAsLong();
	}

	public RandomLoadGenerator() {
		this.min = -1000;
		this.max = 1000;
	}

	@Override
	public long getLoad() {
		return min + (int) (Math.random() * ((max - min) + 1));
	}

}
