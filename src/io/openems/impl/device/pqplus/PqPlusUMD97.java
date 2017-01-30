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
package io.openems.impl.device.pqplus;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.modbus.ModbusDevice;

public class PqPlusUMD97 extends ModbusDevice {

	public final ConfigChannel<PqPlusUMD97Meter> meter = new ConfigChannel<PqPlusUMD97Meter>("meter", this,
			PqPlusUMD97Meter.class);

	public PqPlusUMD97() throws OpenemsException {
		super();
	}

	@Override protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (meter.valueOptional().isPresent()) {
			natures.add(meter.valueOptional().get());
		}
		return natures;
	}

}
