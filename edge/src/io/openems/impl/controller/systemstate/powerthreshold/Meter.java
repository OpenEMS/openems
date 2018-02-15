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
package io.openems.impl.controller.systemstate.powerthreshold;

import java.util.ArrayList;
import java.util.List;

import io.openems.api.channel.ReadChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.device.nature.meter.MeterNature;
import io.openems.api.device.nature.meter.SymmetricMeterNature;

@IsThingMap(type = MeterNature.class)
public class Meter extends ThingMap {

	private List<ReadChannel<Long>> activePowerChannels = new ArrayList<>();

	public Meter(MeterNature meter) {
		super(meter);
		if (meter instanceof SymmetricMeterNature) {
			SymmetricMeterNature m = (SymmetricMeterNature) meter;
			activePowerChannels.add(m.activePower().required());
		} else if (meter instanceof AsymmetricMeterNature) {
			AsymmetricMeterNature m = (AsymmetricMeterNature) meter;
			activePowerChannels.add(m.activePowerL1().required());
			activePowerChannels.add(m.activePowerL2().required());
			activePowerChannels.add(m.activePowerL3().required());
		}
	}

	public Long getPower() {
		Long power = null;
		for (ReadChannel<Long> powerChannel : activePowerChannels) {
			if (powerChannel.valueOptional().isPresent()) {
				if (power == null) {
					power = powerChannel.valueOptional().get();
				} else {
					power += powerChannel.valueOptional().get();
				}
			}
		}
		return power;
	}

}
