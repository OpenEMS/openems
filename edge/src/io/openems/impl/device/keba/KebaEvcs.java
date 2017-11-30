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
package io.openems.impl.device.keba;

import io.openems.api.device.nature.evcs.EvcsNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.keba.KebaDevice;
import io.openems.impl.protocol.keba.KebaDeviceNature;

@ThingInfo(title = "KEBA KeContact EVCS")
public class KebaEvcs extends KebaDeviceNature implements EvcsNature {

	public KebaEvcs(String thingId, KebaDevice parent) throws ConfigException {
		super(thingId, parent);
	}

	@Override
	public String toString() {
		return state.format() + ";" + plug.format() + ";current:" + currUser.format() + "|" + currHardware.format()
		+ ";energy:" + energySession.format() + "|" + energyTotal.format();
	}
}
