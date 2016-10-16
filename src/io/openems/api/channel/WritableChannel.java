package io.openems.api.channel;

import java.math.BigInteger;

public class WritableChannel extends Channel {

	private BigInteger newValue = null;
	private BigInteger newvalueMaxBoundary = null;
	private BigInteger newValueMinBoundary = null;

	public WritableChannel(String unit, BigInteger minValue, BigInteger maxValue) {
		super(unit, minValue, maxValue);
		resetRestrictions();
	}

	public boolean hasNewValue() {
		return newValue != null;
	};

	public BigInteger popNewValue() {
		BigInteger newValue = this.newValue;
		this.newValue = null;
		return newValue;
	}

	/**
	 * Set a new value for this Channel
	 *
	 * @param newValue
	 */
	public void pushNewValue(BigInteger newValue) {
		this.newValue = newValue;
	}

	/**
	 * Needs to be called by Scheduler in the beginning to reset boundaries
	 */
	public void resetRestrictions() {
		this.newValueMinBoundary = minValue;
		this.newvalueMaxBoundary = maxValue;
	}

	public void setNewValueMaxBoundary(BigInteger valueMaxBoundary) {
		this.newvalueMaxBoundary = valueMaxBoundary;
		if (this.newValue != null && this.newValue.compareTo(valueMaxBoundary) > 0) {
			this.newValue = valueMaxBoundary;
		}
	}

	public void setNewValueMinBoundary(BigInteger valueMinBoundary) {
		this.newValueMinBoundary = valueMinBoundary;
		if (this.newValue != null && this.newValue.compareTo(valueMinBoundary) < 0) {
			this.newValue = valueMinBoundary;
		}
	}
}
