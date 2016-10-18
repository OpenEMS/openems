package io.openems.api.channel;

import java.math.BigInteger;
import java.util.Stack;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.WriteChannelException;

/**
 * Defines a writeable {@link Channel}. It handles a specific writeValue or boundaries for a writeValue.
 * Receiving the writeValue behaves similar to a {@link Stack}:
 * - Use {@link setMinWriteValue()} or {@link setMaxWriteValue()} to define a boundary.
 * - Use {@link pushWriteValue()} to set a specific value.
 * - Use {@link hasWriteValue()} to see if a value or a boundary was set.
 * - Use {@link peekWriteValue()} to receive the value.
 * - Use {@link popWriteValue()} to receive the value or a value that was derived from the min and max boundaries and
 * initialize the {@link WriteableChannel}.
 * - The {@link DeviceNature} is internally calling {@link popRawWriteValue()} to receive the value in a format suitable
 * for writing to hardware.
 *
 * @author stefan.feilmeier
 */
public class WriteableChannel extends Channel {
	private BigInteger maxWriteValue = null;
	private BigInteger minWriteValue = null;
	private BigInteger writeValue = null;

	public WriteableChannel(String unit, BigInteger minWriteValue, BigInteger maxWriteValue, BigInteger multiplier,
			BigInteger delta) {
		super(unit, minWriteValue, maxWriteValue, multiplier, delta);
		resetMinMax();
	}

	public BigInteger getMaxWriteValue() {
		return maxWriteValue;
	}

	public BigInteger getMinWriteValue() {
		return minWriteValue;
	};

	public BigInteger getMultiplier() {
		return multiplier;
	}

	public boolean hasWriteValue() {
		return (writeValue != null || minWriteValue != null || maxWriteValue != null);
	}

	public BigInteger peekWriteValue() {
		return writeValue;
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
		writeValue = roundToHardwarePrecision(writeValue);
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
		writeValue = roundToHardwarePrecision(maxValue);
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
		writeValue = roundToHardwarePrecision(minValue);
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

	protected BigInteger popRawWriteValue() {
		return popWriteValue().add(delta).divide(multiplier);
	}

	private BigInteger roundToHardwarePrecision(BigInteger value) {
		BigInteger[] division = value.divideAndRemainder(multiplier);
		if (division[1] != BigInteger.ZERO) {
			BigInteger roundedValue = division[0].multiply(multiplier);
			log.warn("Value [" + value + "] is too precise for device. Will round to [" + roundedValue + "]");
		}
		return value;
	}

}
