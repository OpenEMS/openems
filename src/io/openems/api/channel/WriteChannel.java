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

import java.util.Map.Entry;
import java.util.Optional;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.api.thing.Thing;

public class WriteChannel<T> extends ReadChannel<T> {

	private final Interval<ReadChannel<T>> writeChannelInterval = new Interval<ReadChannel<T>>();
	private final Interval<T> writeInterval = new Interval<T>();
	private Optional<T> writeValue = Optional.empty();

	public WriteChannel(String id, Thing parent) {
		super(id, parent);
	}

	@SuppressWarnings("unchecked") public Optional<T> writeMin() {
		T valueMin = valueInterval().minOptional().orElse(null);
		T writeMin = writeInterval.minOptional().orElse(null);
		T channelMin = null;
		Optional<ReadChannel<T>> minChannelOptional = writeChannelInterval.minOptional();
		if (minChannelOptional.isPresent()) {
			channelMin = minChannelOptional.get().valueOptional().orElse(null);
		}
		T min = valueMin;
		if (min == null || (writeMin != null && writeMin instanceof Comparable
				&& ((Comparable<T>) writeMin).compareTo(min) > 0)) {
			min = writeMin;
		}
		if (min == null || (channelMin != null && channelMin instanceof Comparable
				&& ((Comparable<T>) channelMin).compareTo(min) > 0)) {
			if (channelMin instanceof Long && (Long) channelMin == 0) {
				// TODO. This is a hack. If the storage system is stopped, channel is limited to 0 and making us unable
				// to start the system again
			} else {
				min = channelMin;
			}
		}
		return Optional.ofNullable(min);

	}

	@SuppressWarnings("unchecked") public Optional<T> writeMax() {
		T valueMax = valueInterval().maxOptional().orElse(null);
		T writeMax = writeInterval.maxOptional().orElse(null);
		T channelMax = null;
		Optional<ReadChannel<T>> maxChannelOptional = writeChannelInterval.maxOptional();
		if (maxChannelOptional.isPresent()) {
			channelMax = maxChannelOptional.get().valueOptional().orElse(null);
		}
		T max = valueMax;
		if (max == null || (writeMax != null && writeMax instanceof Comparable
				&& ((Comparable<T>) writeMax).compareTo(max) < 0)) {
			max = writeMax;
		}
		if (max == null || (channelMax != null && channelMax instanceof Comparable
				&& ((Comparable<T>) channelMax).compareTo(max) < 0)) {
			if (channelMax instanceof Long && (Long) channelMax == 0) {
				// TODO. This is a hack. If the storage system is stopped, channel is limited to 0 and making us unable
				// to start the system again
			} else {
				max = channelMax;
			}
		}
		return Optional.ofNullable(max);
	}

	/**
	 * Returns the write interval
	 *
	 * @return max value
	 * @throws InvalidValueException
	 */
	public Interval<T> writeInterval() {
		return new Interval<T>(writeMin(), writeMax());
	}

	/**
	 * Returns the fixed value or one that is derived from max/min boundaries.
	 *
	 * @return value
	 */
	@SuppressWarnings("unchecked") public synchronized Optional<T> peekWrite() {
		if (writeValue.isPresent()) {
			return writeValue;
		} else {
			Optional<T> writeMax = this.writeInterval.maxOptional();
			Optional<T> writeMin = this.writeInterval.minOptional();
			if (writeMin.isPresent() && writeMax.isPresent()) {
				if (writeMin.get() instanceof Number && writeMax.get() instanceof Number) {
					long writeMinLong = ((Number) writeMin.get()).longValue();
					long writeMaxLong = ((Number) writeMax.get()).longValue();
					return (Optional<T>) Optional.of(roundToHardwarePrecision(writeMinLong + writeMaxLong) / 2);
				}
			} else if (writeMin.isPresent()) {
				return writeMin;
			} else if (writeMax.isPresent()) {
				return writeMax;
			}
		}
		return Optional.empty();
	}

	public synchronized Optional<String> peekWriteLabel() {
		Optional<T> value = peekWrite();
		return Optional.ofNullable(labels.get(value));
	}

	public synchronized Optional<T> popWrite() {
		Optional<T> value = peekWrite();
		writeValue = Optional.empty();
		writeInterval.reset();
		return value;
	}

	/**
	 * Returns the fixed value in a format suitable for writing to hardware and initializes the
	 * {@link WriteChannel}. This method is called internally by {@link DeviceNature}.
	 */
	public synchronized Optional<T> popRawWrite() {
		Optional<T> value = popWrite();
		if (!value.isPresent()) {
			// no value present -> return empty Optional
			return value;
		} else if (!multiplier.isPresent() && !delta.isPresent()) {
			// no multiplier and no delta existing
			return value;
		} else if (!(value.get() instanceof Number)) {
			// value is no Long -> return Optional
			return value;
		} else {
			Number number = (Number) value.get();
			long multiplier = 1;
			if (this.multiplier.isPresent()) {
				multiplier = this.multiplier.get();
			}
			long delta = 0;
			if (this.delta.isPresent()) {
				delta = this.delta.get();
			}
			number = (number.longValue() + delta) / multiplier;
			return Optional.of((T) number);
		}
	}

	public void pushWrite(T value) throws WriteChannelException {
		log.debug("Write to [" + address() + "] -> " + value);
		checkIntervalBoundaries(value);
		writeValue = Optional.of(value);
	}

	/**
	 * Set a new value for this Channel using a Label
	 */
	public void pushWriteFromLabel(String label) throws WriteChannelException {
		T result = null;
		for (Entry<T, String> entry : labels.entrySet()) {
			if (entry.getValue().equals(label)) {
				result = entry.getKey();
			}
		}
		if (result == null) {
			throw new WriteChannelException(
					"Label [" + label + "] for Channel [" + address() + "]not found: [" + labels + "]");
		}
		pushWrite(result);
	}

	public void pushWriteMin(T value) throws WriteChannelException {
		checkIntervalBoundaries(value);
		writeInterval.min(value);
	}

	public void pushWriteMax(T value) throws WriteChannelException {
		checkIntervalBoundaries(value);
		writeInterval.max(value);
	}

	@Override public WriteChannel<T> required() {
		return (WriteChannel<T>) super.required();
	}

	@SuppressWarnings("unchecked") private void checkIntervalBoundaries(T value) throws WriteChannelException {
		Optional<T> max = writeMax();
		Optional<T> min = writeMin();
		Optional<T> write = this.writeValue;
		if (!(value instanceof Comparable)) {
			return;
		} else if (write.isPresent() && write.get() instanceof Comparable
				&& ((Comparable<T>) write.get()).compareTo(value) != 0) {
			throw new WriteChannelException("Value [" + value + "] for [" + address()
					+ "] is out of boundaries. Different fixed value [" + write.get() + "] had already been set");
		} else if (max.isPresent() && max.get() instanceof Comparable
				&& ((Comparable<T>) max.get()).compareTo(value) < 0) {
			throw new WriteChannelException("Value [" + value + "] for [" + address()
					+ "] is out of boundaries. Max value [" + max.get() + "] had already been set");
		} else if (min.isPresent() && min.get() instanceof Comparable
				&& ((Comparable<T>) min.get()).compareTo(value) > 0) {
			throw new WriteChannelException("Value [" + value + "] for [" + address()
					+ "] is out of boundaries. Min value [" + min.get() + "] had already been set");
		}
	}

	public WriteChannel<T> minWriteChannel(ReadChannel<T> channel) {
		writeChannelInterval.min(channel);
		return this;
	}

	public WriteChannel<T> maxWriteChannel(ReadChannel<T> channel) {
		writeChannelInterval.max(channel);
		return this;
	}
}
