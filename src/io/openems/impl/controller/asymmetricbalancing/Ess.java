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

import io.openems.api.controller.ThingMap;
import io.openems.api.thing.Thing;

//
// import io.openems.api.channel.IsRequired;
// import io.openems.api.channel.numeric.NumericChannel;
// import io.openems.api.channel.numeric.WriteableNumericChannel;
// import io.openems.api.controller.IsThingMap;
// import io.openems.api.controller.ThingMap;
// import io.openems.api.exception.InvalidValueException;
// import io.openems.impl.device.pro.FeneconProEss;
//
// @IsThingMap(type = FeneconProEss.class)
public class Ess extends ThingMap {

	public Ess(Thing thing) {
		super(thing);
	}
	//
	// @IsRequired(channelId = "ActivePowerPhaseA") public NumericChannel activePowerPhaseA;
	// @IsRequired(channelId = "ActivePowerPhaseB") public NumericChannel activePowerPhaseB;
	// @IsRequired(channelId = "ActivePowerPhaseC") public NumericChannel activePowerPhaseC;
	//
	// @IsRequired(channelId = "AllowedCharge") public NumericChannel allowedCharge;
	//
	// @IsRequired(channelId = "AllowedDischarge") public NumericChannel allowedDischarge;
	//
	// @IsRequired(channelId = "SetActivePowerPhaseA") public WriteableNumericChannel setActivePowerPhaseA;
	//
	// @IsRequired(channelId = "SetReactivePowerPhaseA") public WriteableNumericChannel setReactivePowerPhaseA;
	//
	// @IsRequired(channelId = "SetActivePowerPhaseB") public WriteableNumericChannel setActivePowerPhaseB;
	//
	// @IsRequired(channelId = "SetReactivePowerPhaseB") public WriteableNumericChannel setReactivePowerPhaseB;
	//
	// @IsRequired(channelId = "SetActivePowerPhaseC") public WriteableNumericChannel setActivePowerPhaseC;
	//
	// @IsRequired(channelId = "SetReactivePowerPhaseC") public WriteableNumericChannel setReactivePowerPhaseC;
	//
	// @IsRequired(channelId = "MinSoc") public NumericChannel minSoc;
	//
	// @IsRequired(channelId = "Soc") public NumericChannel soc;
	//
	// @IsRequired(channelId = "SetWorkState") public WriteableNumericChannel setWorkState;
	//
	// public Ess(String thingId) {
	// super(thingId);
	// // TODO Auto-generated constructor stub
	// }
	//
	// public long useableSoc() throws InvalidValueException {
	// return soc.getValue() - minSoc.getValue();
	// }
	//
}
