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
package io.openems.impl.controller.evcs;

import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.device.nature.meter.MeterNature;
import io.openems.api.device.nature.meter.SymmetricMeterNature;

@IsThingMap(type = MeterNature.class)
public class Meter extends ThingMap {
	private final MeterNature meter;

	public Meter(MeterNature meter) {
		super(meter);
		this.meter = meter;

		if (meter instanceof AsymmetricMeterNature) {
			AsymmetricMeterNature m = (AsymmetricMeterNature) meter;
			m.activePowerL1().required();
			m.activePowerL2().required();
			m.activePowerL3().required();
		} else if (meter instanceof SymmetricMeterNature) {
			SymmetricMeterNature m = (SymmetricMeterNature) meter;
			m.activePower().required();
		}
	}

	public long getActivePowerSum() {
		if (this.meter instanceof AsymmetricMeterNature) {
			AsymmetricMeterNature m = (AsymmetricMeterNature) meter;
			return m.activePowerL1().valueOptional().orElse(0l) //
					+ m.activePowerL2().valueOptional().orElse(0l) //
					+ +m.activePowerL3().valueOptional().orElse(0l);
		} else if (meter instanceof SymmetricMeterNature) {
			SymmetricMeterNature m = (SymmetricMeterNature) meter;
			return m.activePower().valueOptional().orElse(0l);
		}
		return 0;
	}
}
