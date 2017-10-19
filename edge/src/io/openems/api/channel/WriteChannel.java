/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
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

import com.google.gson.JsonElement;

import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.exception.WriteChannelException;
import io.openems.api.thing.Thing;
import io.openems.common.exceptions.AccessDeniedException;
import io.openems.common.session.Role;
import io.openems.core.utilities.JsonUtils;

public class WriteChannel<T> extends ReadChannel<T> {

	private final Interval<ReadChannel<T>> writeChannelInterval = new Interval<ReadChannel<T>>();
	private final Interval<T> writeInterval = new Interval<T>();
	private Optional<T> writeValue = Optional.empty();
	protected Optional<T> writeShadowCopy = Optional.empty();

	public WriteChannel(String id, Thing parent) {
		super(id, parent);
	}

	/**
	 * Returns the value as the correct type required by this Channel
	 *
	 * @param j
	 * @return
	 * @throws NotImplementedException
	 */
	public T getAsType(JsonElement j) throws NotImplementedException {
		@SuppressWarnings("unchecked") T result = (T) JsonUtils.getAsType(type().get(), j);
		return result;
	}

	/**
	 * Returns the currently set min value limitation
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Optional<T> writeMin() {
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

	/**
	 * Returns the currently set max value limitation
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Optional<T> writeMax() {
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
	@SuppressWarnings("unchecked")
	public synchronized Optional<T> peekWrite() {
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
		if (value.isPresent()) {
			return Optional.of(labels.get(value.get()));
		} else {
			return Optional.empty();
		}
	}

	public synchronized Optional<T> getWriteValue() {
		return writeValue;
	}

	/**
	 * Stores the write value in a format suitable for writing to hardware and initializes this
	 * {@link WriteChannel}. This method is called internally immediately after the {@link Scheduler} finished all
	 * {@link Controller}s.
	 */
	@SuppressWarnings("unchecked")
	public synchronized void shadowCopyAndReset() {
		Optional<T> value = peekWrite();
		if (value.isPresent() && value.get() instanceof Number && (multiplier.isPresent() || delta.isPresent())) {
			Number number = (Number) value.get();
			long multiplier = 1;
			if (this.multiplier.isPresent()) {
				multiplier = (long) Math.pow(10, this.multiplier.get());
			}
			if (this.negate) {
				multiplier *= -1;
			}
			long delta = 0;
			if (this.delta.isPresent()) {
				delta = this.delta.get();
			}
			number = (number.longValue() + delta) / multiplier;
			value = Optional.of((T) number);
		}
		writeShadowCopy = value;
		writeValue = Optional.empty();
		writeInterval.reset();
	}

	/**
	 * Returns the internal Write-Shadow-Copy and resets it. This method is used just before actually writing the value
	 * to the underlying device.
	 *
	 * @return
	 */
	public synchronized Optional<T> writeShadowCopy() {
		Optional<T> value = writeShadowCopy;
		writeShadowCopy = Optional.empty();
		return value;
	}

	public void pushWrite(T value) throws WriteChannelException {
		log.debug("Write to [" + address() + "] -> " + value);
		checkIntervalBoundaries(value);
		writeValue = Optional.of(value);
	}

	public void pushWriteFromObject(Object valueObj) throws WriteChannelException {
		@SuppressWarnings("unchecked") T value = (T) valueObj;
		this.pushWrite(value);
	}

	public void pushWrite(JsonElement j) throws WriteChannelException, NotImplementedException {
		@SuppressWarnings("unchecked") T value = (T) JsonUtils.getAsType(this.type(), j);
		this.pushWrite(value);
	}

	public void pushWrite(Class<?> type, JsonElement j) throws WriteChannelException, NotImplementedException {
		@SuppressWarnings("unchecked") T value = (T) JsonUtils.getAsType(type, j);
		this.pushWrite(value);
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

	@Override
	public WriteChannel<T> required() {
		return (WriteChannel<T>) super.required();
	}

	@SuppressWarnings("unchecked")
	public void checkIntervalBoundaries(T value) throws WriteChannelException {
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

	@Override
	public String toString() {
		return address() + ", readValue: " + valueOptional().toString() + ",writeValue: " + writeValue.toString();
	}

	@Override
	public boolean isWriteAllowed(Role role) {
		return this.writeRoles().contains(role);
	}

	@Override
	public void assertWriteAllowed(Role role) throws AccessDeniedException {
		if(!isWriteAllowed(role)) {
			throw new AccessDeniedException("User role [" + role.toString().toLowerCase() + "] is not allwed to write channel [" + this.address() + "]");
		}
	}
}
