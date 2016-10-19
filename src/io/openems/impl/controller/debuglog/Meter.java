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
package io.openems.impl.controller.debuglog;

import io.openems.api.channel.Channel;
import io.openems.api.channel.IsRequired;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.MeterNature;

@IsThingMap(type = MeterNature.class)
public class Meter extends ThingMap {

	@IsRequired(channelId = "ActiveNegativeEnergy")
	public Channel activeNegativeEnergy;

	@IsRequired(channelId = "ActivePositiveEnergy")
	public Channel activePositiveEnergy;

	@IsRequired(channelId = "ActivePower")
	public Channel activePower;

	@IsRequired(channelId = "ApparentEnergy")
	public Channel apparentEnergy;

	@IsRequired(channelId = "ApparentPower")
	public Channel apparentPower;

	@IsRequired(channelId = "ReactiveNegativeEnergy")
	public Channel reactiveNegativeEnergy;

	@IsRequired(channelId = "ReactivePositiveEnergy")
	public Channel reactivePositiveEnergy;

	@IsRequired(channelId = "ReactivePower")
	public Channel reactivePower;

	public Meter(String thingId) {
		super(thingId);
	}

	@Override
	public String toString() {
		return "Meter [activePower=" + activePower + ", reactivePower=" + reactivePower + ", apparentPower="
				+ apparentPower + ", activePositiveEnergy=" + activePositiveEnergy + ", activeNegativeEnergy="
				+ activeNegativeEnergy + ", reactivePositiveEnergy=" + reactivePositiveEnergy
				+ ", reactiveNegativeEnergy=" + reactiveNegativeEnergy + ", apparentEnergy=" + apparentEnergy + "]";
	}
}
