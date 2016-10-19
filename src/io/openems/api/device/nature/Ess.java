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
package io.openems.api.device.nature;

import io.openems.api.channel.Channel;
import io.openems.api.channel.IsChannel;
import io.openems.api.channel.WriteableChannel;
import io.openems.api.thing.IsConfig;

public interface Ess extends DeviceNature {
	public final int DEFAULT_MINSOC = 10;

	@IsChannel(id = "ActivePower")
	public Channel activePower();

	@IsChannel(id = "AllowedCharge")
	public Channel allowedCharge();

	@IsChannel(id = "AllowedDischarge")
	public Channel allowedDischarge();

	@IsChannel(id = "ApparentPower")
	public Channel apparentPower();

	@IsChannel(id = "MinSoc")
	public Channel minSoc();

	@IsChannel(id = "RectivePower")
	public Channel reactivePower();

	@IsChannel(id = "SetActivePower")
	public WriteableChannel setActivePower();

	@IsConfig("MinSoc")
	public void setMinSoc(Integer minSoc);

	@IsChannel(id = "SetWorkState")
	public WriteableChannel setWorkState();

	@IsChannel(id = "Soc")
	public Channel soc();

	@IsChannel(id = "SystemState")
	public Channel systemState();
}
