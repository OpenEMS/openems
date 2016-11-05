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

import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.thing.Thing;
import io.vertx.core.impl.ConcurrentHashSet;

public class ReadChannel<T> implements Channel, Comparable<ReadChannel<T>> {
	protected final Logger log;

	private final String id;
	private final Thing parent;
	private Optional<T> value = Optional.empty();

	protected Optional<Long> delta = Optional.empty();
	protected TreeMap<T, String> labels = new TreeMap<T, String>();
	protected Optional<Long> multiplier = Optional.empty();
	private Interval<T> valueInterval = new Interval<T>();
	private String unit = "";

	private final Set<ChannelListener> listeners = new ConcurrentHashSet<>();

	public ReadChannel(String id, Thing parent) {
		log = LoggerFactory.getLogger(this.getClass());
		this.id = id;
		this.parent = parent;
	}

	/*
	 * Builder
	 */
	public ReadChannel<T> interval(T min, T max) {
		this.valueInterval = new Interval<T>(min, max);
		return this;
	}

	public ReadChannel<T> unit(String unit) {
		this.unit = unit;
		return this;
	}

	public ReadChannel<T> multiplier(Long multiplier) {
		this.multiplier = Optional.ofNullable(multiplier);
		return this;
	}

	public ReadChannel<T> listener(ChannelListener... listeners) {
		for (ChannelListener listener : listeners) {
			this.listeners.add(listener);
		}
		return this;
	}

	public ReadChannel<T> label(T value, String label) {
		this.labels.put(value, label);
		return this;
	}

	/*
	 * Getter
	 */
	@Override public String id() {
		return id;
	}

	@Override public String address() {
		return parent.id() + "/" + id;
	}

	@Override public Thing parent() {
		return parent;
	}

	public T value() throws InvalidValueException {
		return valueOptional().orElseThrow(() -> new InvalidValueException("No Value available."));
	};

	public Optional<T> valueOptional() {
		return value;
	};

	public String unitOptional() {
		return unit;
	}

	public void delta(Long delta) {
		this.delta = Optional.ofNullable(delta);
	}

	public Optional<Long> deltaOptional() {
		return delta;
	}

	/**
	 * Update value from the underlying {@link DeviceNature} and send an update event to {@link Databus}.
	 *
	 * @param value
	 */
	protected void updateValue(T value) {
		updateValue(value, true);
	}

	/**
	 * Update value from the underlying {@link DeviceNature}
	 *
	 * @param value
	 * @param triggerEvent
	 *            true if an event should be forwarded to {@link Databus}
	 */
	protected void updateValue(T value, boolean triggerEvent) {
		if (value == null) {
			this.value = Optional.empty();
		}
		if (value instanceof Number && (multiplier.isPresent() || delta.isPresent())) {
			// special treatment for Numbers with given multiplier or delta
			Number number = (Number) value;
			long multiplier = 1;
			if (this.multiplier.isPresent()) {
				multiplier = this.multiplier.get();
			}
			long delta = 0;
			if (this.delta.isPresent()) {
				delta = this.delta.get();
			}
			number = number.longValue() * multiplier - delta;
			this.value = (Optional<T>) Optional.of(number);
		} else {
			this.value = Optional.ofNullable(value);
		}

		if (triggerEvent) {
			listeners.forEach(listener -> listener.channelEvent(this));
		}
	}

	public Optional<String> labelOptional() {
		String label;
		Optional<T> value = valueOptional();
		if (value.isPresent()) {
			label = labels.get(value.get());
			return Optional.ofNullable(label);
		}
		return Optional.empty();
	};

	public String format() {
		Optional<String> label = labelOptional();
		Optional<T> value = valueOptional();
		if (label.isPresent()) {
			return label.get();
		} else if (value.isPresent()) {
			if (unit.equals("")) {
				return value.get().toString();
			} else {
				return value.get() + " " + unit;
			}
		} else {
			return "INVALID";
		}
	}

	public Interval<T> valueInterval() {
		return valueInterval;
	}

	@SuppressWarnings("unchecked") @Override public int compareTo(ReadChannel<T> o) {
		if (this.value.isPresent() && this.value.get() instanceof Comparable && o.value.isPresent()
				&& o.value.get() instanceof Comparable) {
			return ((Comparable<ReadChannel<T>>) this.value.get()).compareTo((ReadChannel<T>) o.value.get());
		} else if (this.value.isPresent()) {
			return 1;
		} else if (o.value.isPresent()) {
			return -1;
		} else {
			return 0;
		}
	}

	/**
	 * Rounds the value to the precision required by hardware. Prints a warning if rounding was necessary.
	 *
	 * @param value
	 * @return rounded value
	 */
	protected synchronized long roundToHardwarePrecision(long value) {
		long multiplier = 1;
		if (this.multiplier.isPresent()) {
			multiplier = this.multiplier.get();
		}
		if (value % multiplier != 0) {
			long roundedValue = (value / multiplier) * multiplier;
			log.warn("Value [" + value + "] is too precise for device. Will round to [" + roundedValue + "]");
		}
		return value;
	}

	/**
	 * Get this channel and mark it as Required. Default is doing nothing, but a subclass might need the information,
	 * which channels are required.
	 *
	 * @return
	 */
	public ReadChannel<T> required() {
		return this;
	};
}
