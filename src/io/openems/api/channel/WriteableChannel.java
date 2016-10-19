package io.openems.api.channel;

import java.math.BigInteger;
import java.util.Stack;

import org.eclipse.jdt.annotation.Nullable;

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

	/**
	 * Returns the set Max boundary.
	 *
	 * @return
	 */
	@Nullable
	public BigInteger getMaxWriteValue() {
		return maxWriteValue;
	}

	/**
	 * Returns the set Min boundary.
	 *
	 * @return
	 */
	@Nullable
	public BigInteger getMinWriteValue() {
		return minWriteValue;
	};

	/**
	 * Returns the multiplier, required to set the value to the hardware
	 *
	 * @return
	 */
	@Nullable
	public BigInteger getMultiplier() {
		return multiplier;
	}

	/**
	 * Checks if a fixed value or a boundary was set.
	 *
	 * @return true if anything was set.
	 */
	public boolean hasWriteValue() {
		return (writeValue != null || minWriteValue != null || maxWriteValue != null);
	}

	/**
	 * Returns the fixed value.
	 *
	 * @return
	 */
	@Nullable
	public BigInteger peekWriteValue() {
		return writeValue;
	}

	/**
	 * Returns the value or a value that was derived from the Min and Max boundaries and initializes the
	 * {@link WriteableChannel}.
	 *
	 * @return
	 */
	@Nullable
	public BigInteger popWriteValue() {
		BigInteger result;
		if (this.writeValue != null) {
			// fixed value exists: return it
			result = this.writeValue;
		} else { // this.writeValue == null
			if (this.maxWriteValue != null) {
				if (this.minWriteValue != null) {
					// Min+Max exist: return average value
					result = this.minWriteValue.add(this.maxWriteValue).divide(BigInteger.valueOf(2));
				} else { // this.minWriteValue == null
					// only Max exists: return it
					result = this.maxWriteValue;
				}
			} else { // this.maxWriteValue == null
				if (this.minWriteValue != null) {
					// only Min exist: return it
					result = this.minWriteValue;
				} else { // this.minWriteValue == null
					// No value exists: return null
					result = null;
				}
			}
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
	public BigInteger pushWriteValue(BigInteger writeValue) throws WriteChannelException {
		writeValue = roundToHardwarePrecision(writeValue);
		checkValueBoundaries(writeValue);
		this.writeValue = writeValue;
		setMinMaxNewValue(writeValue, writeValue);
		return writeValue;
	}

	/**
	 * Needs to be called by Scheduler in the beginning to reset boundaries
	 */
	public void resetMinMax() {
		this.minWriteValue = minValue;
		this.maxWriteValue = maxValue;
	}

	/**
	 * Sets the Max boundary.
	 *
	 * @param maxValue
	 * @return
	 * @throws WriteChannelException
	 */
	public BigInteger setMaxWriteValue(BigInteger maxValue) throws WriteChannelException {
		maxValue = roundToHardwarePrecision(maxValue);
		checkValueBoundaries(maxValue);
		this.maxWriteValue = maxValue;
		return maxValue;
	}

	/**
	 * Sets both Max and Min boundaries
	 *
	 * @param minValue
	 * @param maxValue
	 * @throws WriteChannelException
	 */
	public void setMinMaxNewValue(BigInteger minValue, BigInteger maxValue) throws WriteChannelException {
		setMinWriteValue(minValue);
		setMaxWriteValue(maxValue);
	}

	public BigInteger setMinWriteValue(BigInteger minValue) throws WriteChannelException {
		minValue = roundToHardwarePrecision(minValue);
		checkValueBoundaries(minValue);
		this.minWriteValue = minValue;
		return minValue;
	}

	protected BigInteger popRawWriteValue() {
		BigInteger value = popWriteValue();
		if (value == null) {
			return value;
		}
		return value.add(delta).divide(multiplier);
	}

	private void checkValueBoundaries(BigInteger value) throws WriteChannelException {
		if (this.writeValue != null) {
			if (value.compareTo(this.writeValue) != 0) {
				throwOutOfBoundariesException();
			}
		}
		if (this.minValue != null) {
			if (value.compareTo(this.minValue) < 0) {
				throwOutOfBoundariesException();
			}
		}
		if (this.minWriteValue != null) {
			if (value.compareTo(this.minWriteValue) < 0) {
				throwOutOfBoundariesException();
			}
		}
		if (this.maxValue != null) {
			if (value.compareTo(this.maxValue) > 0) {
				throwOutOfBoundariesException();
			}
		}
		if (this.maxWriteValue != null) {
			if (value.compareTo(this.maxWriteValue) > 0) {
				throwOutOfBoundariesException();
			}
		}
	}

	private BigInteger roundToHardwarePrecision(BigInteger value) {
		BigInteger[] division = value.divideAndRemainder(multiplier);
		if (division[1] != BigInteger.ZERO) {
			BigInteger roundedValue = division[0].multiply(multiplier);
			log.warn("Value [" + value + "] is too precise for device. Will round to [" + roundedValue + "]");
		}
		return value;
	}

	private void throwOutOfBoundariesException() throws WriteChannelException {
		throw new WriteChannelException(
				"Value [" + value + "] is out of boundaries: fixed [" + this.writeValue + "], min [" + this.minValue
						+ "/" + this.minWriteValue + "], max [" + this.maxValue + "/" + this.maxWriteValue + "]");
	}

}
