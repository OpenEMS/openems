/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.api.channel;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Stack;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.InvalidValueException;
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
	protected final Optional<Channel> maxWriteChannel;
	protected final Optional<Channel> minWriteChannel;
	private Optional<Long> maxWriteValue = Optional.empty();
	private Optional<Long> minWriteValue = Optional.empty();
	private Optional<Long> writeValue = Optional.empty();

	public WriteableChannel(DeviceNature nature, String unit, Long minValue, Long maxValue, Long multiplier, Long delta,
			Map<Long, String> labels, Long minWriteValue, Channel minWriteChannel, Long maxWriteValue,
			Channel maxWriteChannel) {
		super(nature, unit, minValue, maxValue, multiplier, delta, labels);
		this.minWriteValue = Optional.ofNullable(minWriteValue);
		this.minWriteChannel = Optional.ofNullable(minWriteChannel);
		this.maxWriteValue = Optional.ofNullable(maxWriteValue);
		this.maxWriteChannel = Optional.ofNullable(maxWriteChannel);
	}

	/**
	 * Returns the maximum allowed write value
	 *
	 * @return max value
	 * @throws InvalidValueException
	 */
	public synchronized Long getAllowedMaxValue() throws InvalidValueException {
		return getAllowedMaxValueOptional()
				.orElseThrow(() -> new InvalidValueException("No Allowed-Max-Value available."));
	}

	public synchronized Optional<Long> getAllowedMaxValueOptional() {
		if (this.writeValue.isPresent()) {
			return this.writeValue;
		} else {
			Optional<Long> value = this.getMaxValueOptional();
			if (!value.isPresent() || (this.maxWriteValue.isPresent() && this.maxWriteValue.get() < value.get())) {
				value = this.maxWriteValue;
			}
			if (this.maxWriteChannel.isPresent()) {
				Optional<Long> maxWriteChannelValue = this.maxWriteChannel.get().getValueOptional();
				if (!value.isPresent() || maxWriteChannelValue.get() < value.get()) {
					value = maxWriteChannelValue;
				}
			}
			return value;
		}
	}

	/**
	 * Returns the minimum allowed write value
	 *
	 * @return min value
	 * @throws InvalidValueException
	 */
	public synchronized Long getAllowedMinValue() throws InvalidValueException {
		return getAllowedMinValueOptional()
				.orElseThrow(() -> new InvalidValueException("No Allowed-Min-Value available."));
	}

	public synchronized Optional<Long> getAllowedMinValueOptional() {
		if (this.writeValue.isPresent()) {
			return this.writeValue;
		} else {
			Optional<Long> value = this.getMinValueOptional();
			if (!value.isPresent() || (this.minWriteValue.isPresent() && this.minWriteValue.get() > value.get())) {
				value = this.minWriteValue;
			}
			if (this.maxWriteChannel.isPresent()) {
				Optional<Long> minWriteChannelValue = this.minWriteChannel.get().getValueOptional();
				if (!value.isPresent() || minWriteChannelValue.get() < value.get()) {
					value = minWriteChannelValue;
				}
			}
			return value;
		}
	}

	public synchronized Optional<Channel> getMaxWriteChannel() {
		return maxWriteChannel;
	}

	public synchronized Optional<Channel> getMinWriteChannel() {
		return minWriteChannel;
	}

	/**
	 * Returns the multiplier, required to set the value to the hardware
	 *
	 * @return value
	 */
	public synchronized Long getMultiplier() {
		return multiplier;
	}

	/**
	 * Checks if a fixed value or a boundary was set.
	 *
	 * @return true if anything was set.
	 */
	public synchronized boolean hasWriteValue() {
		return (writeValue.isPresent() || minWriteValue.isPresent() || maxWriteValue.isPresent());
	}

	/**
	 * Checks if value is within the current boundaries.
	 *
	 * @param value
	 * @return true if allowed
	 */
	public synchronized boolean isAllowed(Long value) {
		Optional<Long> allowedMin = getAllowedMinValueOptional();
		Optional<Long> allowedMax = getAllowedMaxValueOptional();
		if (allowedMin.isPresent() && value < allowedMin.get()) {
			return false;
		} else if (allowedMax.isPresent() && value > allowedMax.get()) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the set Max boundary.
	 *
	 * @return value
	 */
	public synchronized Optional<Long> peekMaxWriteValue() {
		return maxWriteValue;
	}

	/**
	 * Returns the set Min boundary.
	 *
	 * @return value
	 */
	public synchronized Optional<Long> peekMinWriteValue() {
		return minWriteValue;
	}

	/**
	 * Returns the fixed value or one that is derived from max/min boundaries.
	 *
	 * @return value
	 */
	public synchronized Optional<Long> peekWriteValue() {
		if (writeValue.isPresent()) {
			return writeValue;
		} else if (maxWriteValue.isPresent() && minWriteValue.isPresent()) {
			return Optional.of(roundToHardwarePrecision((maxWriteValue.get() + minWriteValue.get()) / 2));
		} else if (maxWriteValue.isPresent()) {
			return maxWriteValue;
		} else if (minWriteValue.isPresent()) {
			return minWriteValue;
		}
		return Optional.empty();
	}

	/**
	 * Returns the fixed value in a format suitable for writing to hardware and initializes the
	 * {@link WriteableChannel}. This method is called internally by {@link DeviceNature}.
	 *
	 * @return value
	 */
	public synchronized Optional<Long> popRawWriteValueOptional() {
		Optional<Long> value = popWriteValueOptional();
		if (!value.isPresent()) {
			return value;
		}
		return Optional.of((value.get() + delta) / multiplier);
	}

	/**
	 * Sets the Max boundary.
	 *
	 * @param maxValue
	 * @return value rounded to hardware requirements
	 * @throws WriteChannelException
	 */
	public synchronized Long pushMaxWriteValue(int maxValue) throws WriteChannelException {
		return pushMaxWriteValue(Long.valueOf(maxValue));
	}

	/**
	 * Sets the Max boundary.
	 *
	 * @param maxValue
	 * @return value rounded to hardware requirements
	 * @throws WriteChannelException
	 */
	public synchronized Long pushMaxWriteValue(Long maxValue) throws WriteChannelException {
		maxValue = roundToHardwarePrecision(maxValue);
		if (!isAllowed(maxValue)) {
			throwOutOfBoundariesException(maxValue);
		}
		this.maxWriteValue = Optional.of(maxValue);
		return maxValue;
	}

	/**
	 * Sets both Max and Min boundaries
	 *
	 * @param minValue
	 * @param maxValue
	 * @throws WriteChannelException
	 */
	public synchronized void pushMinMaxNewValue(Long minValue, Long maxValue) throws WriteChannelException {
		pushMinWriteValue(minValue);
		pushMaxWriteValue(maxValue);
	}

	/**
	 * Sets the Min boundary.
	 *
	 * @param minValue
	 * @return value rounded to hardware requirements
	 * @throws WriteChannelException
	 */
	public synchronized Long pushMinWriteValue(int minValue) throws WriteChannelException {
		return pushMinWriteValue(Long.valueOf(minValue));
	}

	/**
	 * Sets the Min boundary.
	 *
	 * @param minValue
	 * @return value rounded to hardware requirements
	 * @throws WriteChannelException
	 */
	public synchronized Long pushMinWriteValue(Long minValue) throws WriteChannelException {
		minValue = roundToHardwarePrecision(minValue);
		if (!isAllowed(minValue)) {
			throwOutOfBoundariesException(minValue);
		}
		this.minWriteValue = Optional.of(minValue);
		return minValue;
	}

	/**
	 * Set a new value for this Channel
	 *
	 * @param value
	 * @return value rounded to hardware requirements
	 * @throws WriteChannelException
	 */
	public synchronized Long pushWriteValue(int value) throws WriteChannelException {
		return pushWriteValue(Long.valueOf(value));
	}

	/**
	 * Set a new value for this Channel
	 *
	 * @param writeValue
	 * @return value rounded to hardware requirements
	 * @throws WriteChannelException
	 */
	public synchronized Long pushWriteValue(Long value) throws WriteChannelException {
		value = roundToHardwarePrecision(value);
		if (!isAllowed(value)) {
			throwOutOfBoundariesException(value);
		}
		this.writeValue = Optional.of(value);
		pushMinMaxNewValue(value, value);
		return value;
	}

	/**
	 * Set a new value for this Channel using a Label
	 *
	 * @param writeValue
	 * @return value rounded to hardware requirements
	 * @throws WriteChannelException
	 */
	public synchronized Long pushWriteValue(String label) throws WriteChannelException {
		if (!labels.isPresent()) {
			throw new WriteChannelException(
					"Label [" + label + "] not found. No labels set for Channel [" + getAddress() + "]");
		} else if (labels.get().containsKey(label)) {
			throw new WriteChannelException("Label [" + label + "] not found: [" + labels.get() + "]");
		}
		for (Entry<Long, String> entry : labels.get().entrySet()) {
			if (entry.getValue().equals(label)) {
				return pushWriteValue(entry.getKey());
			}
		}
		throw new WriteChannelException("Unexpected error in 'pushWriteValue()'-method with label [" + label
				+ "] for Channel [" + getAddress() + "]");
	}

	/**
	 * Returns the value and initializes the {@link WriteableChannel}.
	 *
	 * @return value
	 */
	private synchronized Optional<Long> popWriteValueOptional() {
		Optional<Long> value = peekWriteValue();
		this.writeValue = Optional.empty();
		this.minWriteValue = Optional.empty();
		this.maxWriteValue = Optional.empty();
		return value;
	}

	/**
	 * Rounds the value to the precision required by hardware. Prints a warning if rounding was necessary.
	 *
	 * @param value
	 * @return rounded value
	 */
	private synchronized Long roundToHardwarePrecision(Long value) {
		if (value % multiplier != 0) {
			Long roundedValue = (value / multiplier) * multiplier;
			log.warn("Value [" + value + "] is too precise for device. Will round to [" + roundedValue + "]");
		}
		return value;
	}

	/**
	 * Helper for checkValueBoundaries() to throw a nice Exception.
	 *
	 * @param value
	 * @throws WriteChannelException
	 */
	private synchronized void throwOutOfBoundariesException(Long value) throws WriteChannelException {
		Optional<Long> minChannelValue = Optional.empty();
		if (minWriteChannel.isPresent()) {
			minChannelValue = minWriteChannel.get().getValueOptional();
		}
		Optional<Long> maxChannelValue = Optional.empty();
		if (maxWriteChannel.isPresent()) {
			maxChannelValue = maxWriteChannel.get().getValueOptional();
		}
		throw new WriteChannelException("Value [" + value + "] is out of boundaries: fixed [" + this.writeValue
				+ "], min [" + this.getMinValueOptional() + "/" + this.minWriteValue + "/" + minChannelValue
				+ "], max [" + this.getMaxValueOptional() + "/" + this.maxWriteValue + "/" + maxChannelValue + "]");
	}

}
