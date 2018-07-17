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
package io.openems.core.utilities.hysteresis;

import io.openems.core.utilities.AvgFiFoQueue;

public class Hysteresis {

	private final Long min;
	private final Long max;
	private AvgFiFoQueue queue = new AvgFiFoQueue(10, 1.5);

	public Hysteresis(long min, long max) {
		this.min = min;
		this.max = max;
	}

	public void apply(long value, HysteresisFunctional func) {
		if (queue.lastAddedValue() == null || queue.lastAddedValue() != value) {
			queue.add(value);
		}
		long pos = queue.avg() - min;
		// Check if value is in hysteresis
		double multiplier = (double) pos / (double) (max - min);
		HysteresisState state = HysteresisState.ABOVE;
		if (pos >= 0 && queue.avg() <= max) {
			// in Hysteresis
			if (queue.avg() < value) {
				// Ascending
				state = HysteresisState.ASC;
			} else {
				// Descending
				state = HysteresisState.DESC;
			}
		} else if (pos < 0) {
			// Below Hysteresis
			state = HysteresisState.BELOW;
		}
		func.function(state, multiplier);
	}

}
