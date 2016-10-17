package io.openems.api.channel;

import java.math.BigInteger;

public class ChannelBuilder<B extends ChannelBuilder<?>> {
	protected BigInteger maxValue = null;
	protected BigInteger minValue = null;
	protected String unit = "";

	public Channel build() {
		return new Channel(unit, minValue, maxValue);
	}

	@SuppressWarnings("unchecked")
	public B maxValue(BigInteger maxValue) {
		this.maxValue = maxValue;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B maxValue(int maxValue) {
		this.maxValue = BigInteger.valueOf(maxValue);
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B minValue(BigInteger minValue) {
		this.minValue = minValue;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B minValue(int minValue) {
		this.minValue = BigInteger.valueOf(minValue);
		return (B) this;
	}

	public B percentType() {
		maxValue(100);
		minValue(0);
		return unit("%");
	}

	@SuppressWarnings("unchecked")
	public B unit(String unit) {
		this.unit = unit;
		return (B) this;
	}

}
