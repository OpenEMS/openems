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
package io.openems.impl.controller.asymmetricbalancing;

import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.thing.Thing;
import io.openems.impl.device.socomec.SocomecMeter;

@IsThingMap(type = SocomecMeter.class)
public class Meter extends ThingMap {

	public Meter(Thing thing) {
		super(thing);
	}
	//
	// @IsRequired(channelId = "ActivePowerPhaseA") public NumericChannel activePowerPhaseA;
	//
	// @IsRequired(channelId = "ActivePowerPhaseB") public NumericChannel activePowerPhaseB;
	//
	// @IsRequired(channelId = "ActivePowerPhaseC") public NumericChannel activePowerPhaseC;

}
