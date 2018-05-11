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
package io.openems.impl.device.streetscooter;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.common.exceptions.OpenemsException;
import io.openems.impl.protocol.modbus.ModbusDevice;

@ThingInfo(title = "FENECON Pro")
public class Streetscooter extends ModbusDevice {

	/*
	 * Constructors
	 */
	public Streetscooter(Bridge parent) throws OpenemsException {
		super(parent);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess 0", description = "Sets the first Ess nature.", type = StreetscooterEss1.class)
	public final ConfigChannel<StreetscooterEss1> ess0 = new ConfigChannel<StreetscooterEss1>("ess0", this)
	.addChangeListener(this);

	@ChannelInfo(title = "Ess 1", description = "Sets the second Ess nature.", type = StreetscooterEss2.class)
	public final ConfigChannel<StreetscooterEss2> ess1 = new ConfigChannel<StreetscooterEss2>("ess1", this)
	.addChangeListener(this);

	/*
	 * Methods
	 */
	@Override
	public String toString() {
		return "Streetscooter [ess0=" + ess0 + ", ess1=" + ess1 + ", getThingId()=" + id() + "]";
	}

	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (ess0.valueOptional().isPresent()) {
			natures.add(ess0.valueOptional().get());
		}
		if (ess1.valueOptional().isPresent()) {
			natures.add(ess1.valueOptional().get());
		}
		return natures;
	}

}
