package io.openems.api.channel;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import io.openems.api.device.nature.DeviceNature;

public class ChannelBuilder<B extends ChannelBuilder<?>> {
	protected BigInteger delta = BigInteger.ZERO;
	protected Map<BigInteger, String> labels;
	protected BigInteger maxValue = null;
	protected BigInteger minValue = null;
	protected BigInteger multiplier = BigInteger.ONE;
	protected DeviceNature nature = null;
	protected String unit = "";

	public Channel build() {
		return new Channel(nature, unit, minValue, maxValue, multiplier, delta, labels);
	}

	@SuppressWarnings("unchecked")
	public B delta(BigInteger delta) {
		this.delta = delta;
		return (B) this;
	}

	public B delta(int delta) {
		return delta(BigInteger.valueOf(delta));
	}

	@SuppressWarnings("unchecked")
	public B label(BigInteger value, String label) {
		if (this.labels == null) {
			this.labels = new HashMap<>();
		}
		this.labels.put(value, label);
		return (B) this;
	}

	public B label(int value, String label) {
		return label(BigInteger.valueOf(value), label);
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

	@SuppressWarnings("unchecked")
	public B multiplier(BigInteger multiplier) {
		this.multiplier = multiplier;
		return (B) this;
	}

	public B multiplier(int multiplier) {
		return multiplier(BigInteger.valueOf(multiplier));
	}

	@SuppressWarnings("unchecked")
	public B nature(DeviceNature nature) {
		this.nature = nature;
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
