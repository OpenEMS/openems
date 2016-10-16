package io.openems.api.channel;

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.exception.InvalidValueException;
import io.openems.core.databus.DataBus;

public class Channel {
	private final static Logger log = LoggerFactory.getLogger(Channel.class);
	protected BigInteger maxValue = null;
	protected BigInteger minValue = null;
	protected BigInteger value;
	private DataBus dataBus = null;
	private boolean isValid = false;
	private final String unit;

	public Channel(String unit, BigInteger minValue, BigInteger maxValue) {
		this.unit = unit;
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

	public void setDataBus(DataBus dataBus) {
		this.dataBus = dataBus;
	}

	@Override
	public String toString() {
		if (isValid) {
			return "Channel [value=" + value + " " + unit + ", max=" + maxValue + ", min=" + minValue + "]";
		} else {
			return "Channel [value=INVALID, unit=" + unit + ", max=" + maxValue + ", min=" + minValue + "]";
		}
	}

	protected void updateValue(BigInteger value) {
		if (value == null) {
			this.isValid = false;
		} else {
			this.isValid = true;
		}
		this.value = value;
		if (dataBus != null) {
			dataBus.channelValueUpdated(this);
		}
	}

}
