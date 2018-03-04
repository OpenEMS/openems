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
package io.openems.impl.controller.symmetric.avoidtotaldischargesoctimeline;

import java.time.LocalTime;
import java.util.Map;
import java.util.TreeMap;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.exception.InvalidValueException;

@IsThingMap(type = SymmetricEssNature.class)
public class Ess extends ThingMap {

	public final ReadChannel<Integer> minSoc;
	public final WriteChannel<Long> setActivePower;
	public final ReadChannel<Long> soc;
	public final ReadChannel<Long> systemState;
	public int maxPowerPercent = 100;
	public final ReadChannel<Long> allowedDischarge;
	public final ReadChannel<Integer> chargeSoc;
	private TreeMap<LocalTime, Soc> timeline = new TreeMap<>();
	public State currentState = State.NORMAL;

	public enum State {
		NORMAL, MINSOC, CHARGESOC
	}

	public Ess(SymmetricEssNature ess) {
		super(ess);
		setActivePower = ess.setActivePower().required();
		systemState = ess.systemState().required();
		soc = ess.soc().required();
		minSoc = ess.minSoc().required();
		allowedDischarge = ess.allowedDischarge().required();
		chargeSoc = ess.chargeSoc().required();
	}

	public void addTime(LocalTime time, int minSoc, int chargeSoc) {
		Soc soc = new Soc(minSoc, chargeSoc);
		timeline.put(time, soc);
	}

	public int getMinSoc(LocalTime time) throws InvalidValueException {
		Map.Entry<LocalTime, Soc> entry = timeline.floorEntry(time);
		if (entry != null) {
			return entry.getValue().minSoc;
		}
		entry = timeline.lastEntry();
		if (entry != null) {
			return entry.getValue().minSoc;
		}
		return minSoc.value();
	}

	public int getChargeSoc(LocalTime time) throws InvalidValueException {
		Map.Entry<LocalTime, Soc> entry = timeline.floorEntry(time);
		if (entry != null) {
			return entry.getValue().chargeSoc;
		}
		entry = timeline.lastEntry();
		if (entry != null) {
			return entry.getValue().chargeSoc;
		}
		return chargeSoc.value();
	}

	private class Soc {
		public final int minSoc;
		public final int chargeSoc;

		public Soc(int minSoc, int chargeSoc) {
			super();
			this.minSoc = minSoc;
			this.chargeSoc = chargeSoc;
		}

	}
}
