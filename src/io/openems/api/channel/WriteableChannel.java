package io.openems.api.channel;

import java.math.BigInteger;

import io.openems.api.exception.WriteChannelException;

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
	 * @throws WriteChannelException
	 */
	public void pushWriteValue(BigInteger writeValue) throws WriteChannelException {
		if ((minWriteValue == null || (minWriteValue != null && writeValue.compareTo(minWriteValue) > 0))
				&& (maxWriteValue == null || (maxWriteValue != null && writeValue.compareTo(maxWriteValue) < 0))) {
			this.writeValue = writeValue;
			setMinMaxNewValue(writeValue, writeValue);
		} else {
			throw new WriteChannelException("Value [" + writeValue + "] is out of boundaries: min [" + minWriteValue
					+ "] max [" + maxWriteValue + "]");
		}
	}

	/**
	 * Needs to be called by Scheduler in the beginning to reset boundaries
	 */
	public void resetMinMax() {
		this.minWriteValue = minValue;
		this.maxWriteValue = maxValue;
	}

	public void setMaxWriteValue(BigInteger maxValue) throws WriteChannelException {
		if ((this.minWriteValue == null
				|| (this.minWriteValue != null && writeValue.compareTo(this.minWriteValue) >= 0))
				&& (this.maxWriteValue == null
						|| (this.maxWriteValue != null && writeValue.compareTo(this.maxWriteValue) <= 0))) {
			this.maxWriteValue = maxValue;
		} else {
			throw new WriteChannelException("Max-Value [" + maxValue + "] is out of boundaries: min ["
					+ this.minWriteValue + "] max [" + this.maxWriteValue + "]");
		}
	}

	public void setMinMaxNewValue(BigInteger minValue, BigInteger maxValue) throws WriteChannelException {
		setMinWriteValue(minValue);
		setMaxWriteValue(maxValue);
	}

	public void setMinWriteValue(BigInteger minValue) throws WriteChannelException {
		if ((this.minWriteValue == null
				|| (this.minWriteValue != null && writeValue.compareTo(this.minWriteValue) >= 0))
				&& (this.maxWriteValue == null
						|| (this.maxWriteValue != null && writeValue.compareTo(this.maxWriteValue) <= 0))) {
			this.minWriteValue = minValue;
		} else {
			throw new WriteChannelException("Min-Value [" + minValue + "] is out of boundaries: min ["
					+ this.minWriteValue + "] max [" + this.maxWriteValue + "]");
		}
	}
}
