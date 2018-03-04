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

import io.openems.api.exception.InvalidValueException;

public class Interval<T> {
	private Optional<T> min = Optional.empty();
	private Optional<T> max = Optional.empty();

	public Interval() {
	}

	public Interval(Optional<T> min, Optional<T> max) {
		this.min = min;
		this.max = max;
	}

	public Interval(T min, T max) {
		this.min = Optional.ofNullable(min);
		this.max = Optional.ofNullable(max);
	}

	public void min(T value) {
		this.min = Optional.ofNullable(value);
	}

	public void max(T value) {
		this.max = Optional.ofNullable(value);
	}

	public T max() throws InvalidValueException {
		return max.orElseThrow(() -> new InvalidValueException("No Max-Value available."));
	}

	public Optional<T> maxOptional() {
		return max;
	}

	public T min() throws InvalidValueException {
		return min.orElseThrow(() -> new InvalidValueException("No Min-Value available."));
	}

	public Optional<T> minOptional() {
		return min;
	}

	public boolean isPresent() {
		return min.isPresent() && max.isPresent();
	}

	@SuppressWarnings("unchecked")
	public boolean isAboveMin(T value) {
		if (min.isPresent() && min.get() instanceof Comparable) {
			return ((Comparable<T>) value).compareTo(min.get()) >= 0;
		} else {
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	public boolean isBelowMax(T value) {
		if (max.isPresent() && max.get() instanceof Comparable) {
			return ((Comparable<T>) value).compareTo(max.get()) <= 0;
		} else {
			return true;
		}
	}

	public boolean isWithinInterval(T value) {
		return isAboveMin(value) && isBelowMax(value);
	}

	public void reset() {
		min = Optional.empty();
		max = Optional.empty();
	}
}
