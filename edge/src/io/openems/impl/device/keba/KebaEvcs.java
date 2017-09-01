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

import java.util.ArrayList;
import java.util.List;

import io.openems.api.channel.ReadChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.evcs.EvcsNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.keba.KebaDeviceNature;
import io.openems.impl.protocol.keba.KebaReadChannel;

@ThingInfo(title = "KEBA KeContact EVCS")
public class KebaEvcs extends KebaDeviceNature implements EvcsNature {

	/*
	 * Constructors
	 */
	public KebaEvcs(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		log.info("Constructor KebaEvcs");
	}

	/*
	 * Inherited Channels
	 */
	private KebaReadChannel<Long> current = new KebaReadChannel<Long>("Current", this);

	@Override
	public ReadChannel<Long> current() {
		return current;
	}

	/*
	 * This Channels
	 */
	private KebaReadChannel<String> product = new KebaReadChannel<String>("Product", this);
	private KebaReadChannel<String> serial = new KebaReadChannel<String>("Serial", this);

	@Override
	protected List<String> getWriteMessages() {
		// TODO Auto-generated method stub
		return new ArrayList<String>();
	}
}
