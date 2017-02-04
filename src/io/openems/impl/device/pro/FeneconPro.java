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
package io.openems.impl.device.pro;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.modbus.ModbusDevice;

@ThingInfo("Represents a FENECON Pro device")
public class FeneconPro extends ModbusDevice {

	/*
	 * Config
	 */
	@ConfigInfo(title = "Sets the ess nature", type = FeneconProEss.class)
	public final ConfigChannel<FeneconProEss> ess = new ConfigChannel<FeneconProEss>("ess", this);

	@ConfigInfo(title = "Sets the meter nature", type = FeneconProEss.class)
	public final ConfigChannel<FeneconProPvMeter> meter = new ConfigChannel<FeneconProPvMeter>("meter", this);

	public FeneconPro() throws OpenemsException {
		super();
	}

	@Override
	public String toString() {
		return "FeneconPro [ess=" + ess + ", getThingId()=" + id() + "]";
	}

	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (ess.valueOptional().isPresent()) {
			natures.add(ess.valueOptional().get());
		}
		if (meter.valueOptional().isPresent()) {
			natures.add(meter.valueOptional().get());
		}
		return natures;
	}
}
