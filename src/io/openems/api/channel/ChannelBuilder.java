package io.openems.api.channel;

import java.math.BigInteger;

public class ChannelBuilder<T extends ChannelBuilder<?>> {
	protected BigInteger maxValue = null;
	protected BigInteger minValue = null;
	protected String unit = "";

	public Channel build() {
		// if (address == null) {
		// throw new OpenemsModbusException("Error in protocol: [address] is missing");
		// } else if (channel == null) {
		// throw new OpenemsModbusException("Error in protocol: [channel] is missing");
		// }
		// ;
		return new Channel(unit, minValue, maxValue);
	}

	@SuppressWarnings("unchecked")
	public T maxValue(BigInteger maxValue) {
		this.maxValue = maxValue;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T maxValue(int maxValue) {
		this.maxValue = BigInteger.valueOf(maxValue);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T minValue(BigInteger minValue) {
		this.minValue = minValue;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T minValue(int minValue) {
		this.minValue = BigInteger.valueOf(minValue);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T unit(String unit) {
		this.unit = unit;
		return (T) this;
	}

}
