package io.openems.api.channel;

import java.math.BigInteger;

public class WriteableChannel extends Channel {

	private BigInteger maxWriteValue = null;
	private BigInteger minWriteValue = null;
	private BigInteger writeValue = null;

	public WriteableChannel(String unit, BigInteger minWriteValue, BigInteger maxWriteValue) {
		super(unit, minWriteValue, maxWriteValue);
		resetMinMax();
	}

	public BigInteger getMaxWriteValue() {
		return maxWriteValue;
	};

	public BigInteger getMinWriteValue() {
		return minWriteValue;
	}

	public boolean hasWriteValue() {
		return (writeValue != null || minWriteValue != null || maxWriteValue != null);
	}

	public BigInteger popWriteValue() {
		BigInteger result;
		if (this.writeValue != null) {
			result = this.writeValue;
		} else if (this.maxWriteValue != null && this.minWriteValue != null) {
			result = this.minWriteValue.add(this.maxWriteValue).divide(BigInteger.valueOf(2));
		} else if (this.maxWriteValue != null) {
			result = this.minValue;
		} else {
			// (this.newMinValue != null)
			result = this.maxValue;
		}
		this.writeValue = null;
		this.minWriteValue = null;
		this.maxWriteValue = null;
		return result;
	}

	/**
	 * Set a new value for this Channel
	 *
	 * @param writeValue
	 */
	public void pushWriteValue(BigInteger writeValue) {
		this.writeValue = writeValue;
		setMinMaxNewValue(writeValue, writeValue);
	}

	/**
	 * Needs to be called by Scheduler in the beginning to reset boundaries
	 */
	public void resetMinMax() {
		this.minWriteValue = minValue;
		this.maxWriteValue = maxValue;
	}

	public void setMaxWriteValue(BigInteger maxValue) {
		this.maxWriteValue = maxValue;
		if (this.writeValue != null && this.writeValue.compareTo(maxValue) > 0) {
			this.writeValue = maxValue;
		}
	}

	public void setMinMaxNewValue(BigInteger minValue, BigInteger maxValue) {
		setMinWriteValue(minValue);
		setMaxWriteValue(maxValue);
	}

	public void setMinWriteValue(BigInteger minValue) {
		this.minWriteValue = minValue;
		if (this.writeValue != null && this.writeValue.compareTo(minValue) < 0) {
			this.writeValue = minValue;
		}
	}
}
