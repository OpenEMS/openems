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
	protected final Channel maxWriteChannel;
	private Long maxWriteValue = null;
	protected final Channel minWriteChannel;
	private Long minWriteValue = null;
	private Long writeValue = null;

	public WriteableChannel(DeviceNature nature, String unit, Long minValue, Long maxValue, Long multiplier, Long delta,
			Map<Long, String> labels, Long minWriteValue, Channel minWriteChannel, Long maxWriteValue,
			Channel maxWriteChannel) {
		super(nature, unit, minValue, maxValue, multiplier, delta, labels);
		this.minWriteValue = minWriteValue;
		this.minWriteChannel = minWriteChannel;
		this.maxWriteValue = maxWriteValue;
		this.maxWriteChannel = maxWriteChannel;
	}

	/**
	 * Returns the maximum allowed write value
	 *
	 * @return
	 */
	public Long getAllowedMaxValue() {
		if (this.writeValue != null) {
			return this.writeValue;
		} else {
			Long value = Long.MAX_VALUE;
			if (this.getMaxValue() != null) {
				value = this.getMaxValue();
			}
			if (this.maxWriteValue != null) {
				if (value == null || this.maxWriteValue < value) {
					value = this.maxWriteValue;
				}
			}
			if (this.maxWriteChannel != null) {
				Long maxWriteChannelValue = this.maxWriteChannel.getValueOrNull();
				if (value == null || (maxWriteChannelValue != null && maxWriteChannelValue < value)) {
					value = maxWriteChannelValue;
				}
			}
			return value;
		}
	}

	/**
	 * Returns the minimum allowed write value
	 *
	 * @return
	 */
	public Long getAllowedMinValue() {
		if (this.writeValue != null) {
			return this.writeValue;
		} else {
			Long value = Long.MIN_VALUE;
			if (this.getMinValue() != null) {
				value = this.getMinValue();
			}
			if (this.minWriteValue != null) {
				if (value == null || this.minWriteValue > value) {
					value = this.minWriteValue;
				}
			}
			if (this.minWriteChannel != null) {
				Long minWriteChannelValue = this.minWriteChannel.getValueOrNull();
				if (value == null || (minWriteChannelValue != null && minWriteChannelValue > value)) {
					value = minWriteChannelValue;
				}
			}
			return value;
		}
	}

	public Channel getMaxWriteChannel() {
		return maxWriteChannel;
	}

	public Channel getMinWriteChannel() {
		return minWriteChannel;
	}

	/**
	 * Returns the multiplier, required to set the value to the hardware
	 *
	 * @return value
	 */
	public Long getMultiplier() {
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
	 * Checks if value is within the current boundaries.
	 *
	 * @param value
	 * @return true if allowed
	 */
	public boolean isAllowed(Long value) {
		if (value < getAllowedMinValue()) {
			return false;
		} else if (value > getAllowedMaxValue()) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the set Max boundary.
	 *
	 * @return value or null
	 */
	public Long peekMaxWriteValue() {
		return maxWriteValue;
	}

	/**
	 * Returns the set Min boundary.
	 *
	 * @return value or null
	 */
	public Long peekMinWriteValue() {
		return minWriteValue;
	}

	/**
	 * Returns the fixed value or one that is derived from max/min boundaries.
	 *
	 * @return value or null
	 */
	public Long peekWriteValue() {
		if (writeValue != null) {
			return writeValue;
		} else if (maxWriteValue != null && minWriteValue != null) {
			return roundToHardwarePrecision((maxWriteValue + minWriteValue) / 2);
		} else if (maxWriteValue != null) {
			return maxWriteValue;
		} else if (minWriteValue != null) {
			return minWriteValue;
		}
		return null;
	}

	/**
	 * Returns the fixed value in a format suitable for writing to hardware and initializes the
	 * {@link WriteableChannel}. This method is called internally by {@link DeviceNature}.
	 *
	 * @return value or null
	 */
	public Long popRawWriteValue() {
		Long value = popWriteValue();
		if (value == null) {
			return value;
		}
		long rawValue = (value + delta) / multiplier;
		return rawValue;
	}

	/**
	 * Returns the value and initializes the {@link WriteableChannel}.
	 *
	 * @return value or null
	 */
	private Long popWriteValue() {
		Long value = peekWriteValue();
		this.writeValue = null;
		this.minWriteValue = null;
		this.maxWriteValue = null;
		return value;
	}

	/**
	 * Sets the Max boundary.
	 *
	 * @param maxValue
	 * @return value rounded to hardware requirements
	 * @throws WriteChannelException
	 */
	public Long pushMaxWriteValue(int maxValue) throws WriteChannelException {
		return pushMaxWriteValue(Long.valueOf(maxValue));
	}

	/**
	 * Sets the Max boundary.
	 *
	 * @param maxValue
	 * @return value rounded to hardware requirements
	 * @throws WriteChannelException
	 */
	public Long pushMaxWriteValue(Long maxValue) throws WriteChannelException {
		maxValue = roundToHardwarePrecision(maxValue);
		if (!isAllowed(maxValue)) {
			throwOutOfBoundariesException(maxValue);
		}
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
	public void pushMinMaxNewValue(Long minValue, Long maxValue) throws WriteChannelException {
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
	public Long pushMinWriteValue(int minValue) throws WriteChannelException {
		return pushMinWriteValue(Long.valueOf(minValue));
	}

	/**
	 * Sets the Min boundary.
	 *
	 * @param minValue
	 * @return value rounded to hardware requirements
	 * @throws WriteChannelException
	 */
	public Long pushMinWriteValue(Long minValue) throws WriteChannelException {
		minValue = roundToHardwarePrecision(minValue);
		if (!isAllowed(minValue)) {
			throwOutOfBoundariesException(minValue);
		}
		this.minWriteValue = minValue;
		return minValue;
	}

	/**
	 * Set a new value for this Channel
	 *
	 * @param value
	 * @return value rounded to hardware requirements
	 * @throws WriteChannelException
	 */
	public Long pushWriteValue(int value) throws WriteChannelException {
		return pushWriteValue(Long.valueOf(value));
	}

	/**
	 * Set a new value for this Channel
	 *
	 * @param writeValue
	 * @return value rounded to hardware requirements
	 * @throws WriteChannelException
	 */
	public Long pushWriteValue(Long value) throws WriteChannelException {
		value = roundToHardwarePrecision(value);
		if (!isAllowed(value)) {
			throwOutOfBoundariesException(value);
		}
		this.writeValue = value;
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
	public Long pushWriteValue(String label) throws WriteChannelException {
		if (labels == null) {
			throw new WriteChannelException(
					"Label [" + label + "] not found. No labels set for Channel [" + getAddress() + "]");
		} else if (!labels.containsValue(label)) {
			throw new WriteChannelException("Label [" + label + "] not found: [" + labels.values() + "]");
		}
		for (Entry<Long, String> entry : labels.entrySet()) {
			if (entry.getValue().equals(label)) {
				return pushWriteValue(entry.getKey());
			}
		}
		throw new WriteChannelException("Unexpected error in 'pushWriteValue()'-method with label [" + label
				+ "] for Channel [" + getAddress() + "]");
	}

	/**
	 * Rounds the value to the precision required by hardware. Prints a warning if rounding was necessary.
	 *
	 * @param value
	 * @return rounded value
	 */
	private Long roundToHardwarePrecision(Long value) {
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
	private void throwOutOfBoundariesException(Long value) throws WriteChannelException {
		Long minChannelValue = null;
		if (minWriteChannel != null) {
			minChannelValue = minWriteChannel.getValueOrNull();
		}
		Long maxChannelValue = null;
		if (maxWriteChannel != null) {
			maxChannelValue = maxWriteChannel.getValueOrNull();
		}
		throw new WriteChannelException("Value [" + value + "] is out of boundaries: fixed [" + this.writeValue
				+ "], min [" + this.getMinValue() + "/" + this.minWriteValue + "/" + minChannelValue + "], max ["
				+ this.getMaxValue() + "/" + this.maxWriteValue + "/" + maxChannelValue + "]");
	}

}
