package io.openems.api.channel;

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.databus.Databus;

public class Channel {
	protected final static Logger log = LoggerFactory.getLogger(Channel.class);
	protected final BigInteger delta;
	protected BigInteger maxValue = null;
	protected BigInteger minValue = null;
	protected final BigInteger multiplier;
	protected BigInteger value;
	private Databus databus = null;
	private boolean isValid = false;
	private final String unit;

	public Channel(String unit, BigInteger minValue, BigInteger maxValue, BigInteger multiplier, BigInteger delta) {
		this.unit = unit;
		this.multiplier = multiplier;
		this.delta = delta;
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	public BigInteger getMaxValue() {
		return maxValue;
	}

	public BigInteger getMinValue() {
		return minValue;
	};

	public BigInteger getValue() throws InvalidValueException {
		if (this.isValid) {
			return value;
		} else {
			throw new InvalidValueException("Channel value is invalid.");
		}
	};

	public BigInteger getValueOrNull() {
		return value;
	}

	public void setDatabus(Databus databus) {
		this.databus = databus;
	}

	public String toSimpleString() {
		if (isValid) {
			return value + " " + unit;
		} else {
			return "INVALID";
		}
	}

	@Override
	public String toString() {
		if (isValid) {
			return "Channel [value=" + value + " " + unit + ", max=" + maxValue + ", min=" + minValue + "]";
		} else {
			return "Channel [value=INVALID, unit=" + unit + ", max=" + maxValue + ", min=" + minValue + "]";
		}
	}

	/**
	 * Update value from the underlying {@link DeviceNature} and send an update event to {@link Databus}.
	 *
	 * @param value
	 */
	protected void updateValue(BigInteger value) {
		updateValue(value, true);
	}

	/**
	 * Update value from the underlying {@link DeviceNature}
	 *
	 * @param value
	 * @param triggerDatabusEvent
	 *            true if an event should be forwarded to {@link Databus}
	 */
	protected void updateValue(BigInteger value, boolean triggerDatabusEvent) {
		if (value == null) {
			this.isValid = false;
		} else {
			this.isValid = true;
		}
		this.value = value.multiply(multiplier).subtract(delta);
		if (databus != null && triggerDatabusEvent) {
			databus.channelValueUpdated(this);
		}
	}
}
