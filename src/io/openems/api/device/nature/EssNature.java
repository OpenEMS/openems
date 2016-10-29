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

import io.openems.api.channel.IsChannel;
import io.openems.api.channel.NumericChannel;
import io.openems.api.channel.WriteableNumericChannel;
import io.openems.api.thing.IsConfig;

public interface EssNature extends DeviceNature {
	public final int DEFAULT_MINSOC = 10;

	public final String OFF_GRID = "Off-Grid";
	public final String ON_GRID = "On-Grid";

	public final String STANDBY = "Standby";
	public final String START = "Start";
	public final String STOP = "Stop";

	@IsChannel(id = "ActivePower")
	public NumericChannel activePower();

	@IsChannel(id = "AllowedCharge")
	public NumericChannel allowedCharge();

	@IsChannel(id = "AllowedDischarge")
	public NumericChannel allowedDischarge();

	@IsChannel(id = "ApparentPower")
	public NumericChannel apparentPower();

	@IsChannel(id = "GridMode")
	public NumericChannel gridMode();

	@IsChannel(id = "MinSoc")
	public NumericChannel minSoc();

	@IsChannel(id = "ReactivePower")
	public NumericChannel reactivePower();

	@IsChannel(id = "SetActivePower")
	public WriteableNumericChannel setActivePower();

	@IsConfig("MinSoc")
	public void setMinSoc(Integer minSoc);

	@IsChannel(id = "SetWorkState")
	public WriteableNumericChannel setWorkState();

	@IsChannel(id = "Soc")
	public NumericChannel soc();

	@IsChannel(id = "SystemState")
	public NumericChannel systemState();
}
