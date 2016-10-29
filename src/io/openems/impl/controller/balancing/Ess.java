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
package io.openems.impl.controller.balancing;

import io.openems.api.channel.IsRequired;
import io.openems.api.channel.numeric.NumericChannel;
import io.openems.api.channel.numeric.WriteableNumericChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.EssNature;
import io.openems.api.exception.InvalidValueException;

@IsThingMap(type = EssNature.class)
public class Ess extends ThingMap {

	@IsRequired(channelId = "ActivePower")
	public NumericChannel activePower;

	@IsRequired(channelId = "AllowedCharge")
	public NumericChannel allowedCharge;

	@IsRequired(channelId = "AllowedDischarge")
	public NumericChannel allowedDischarge;

	@IsRequired(channelId = "GridMode")
	public NumericChannel gridMode;

	@IsRequired(channelId = "MinSoc")
	public NumericChannel minSoc;

	@IsRequired(channelId = "SetActivePower")
	public WriteableNumericChannel setActivePower;

	@IsRequired(channelId = "Soc")
	public NumericChannel soc;

	public Ess(String thingId) {
		super(thingId);
	}

	public long useableSoc() throws InvalidValueException {
		return soc.getValue() - minSoc.getValue();
	}
}
