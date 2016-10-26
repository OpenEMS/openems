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

import io.openems.api.device.nature.IsDeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.IsConfig;
import io.openems.impl.protocol.modbus.ModbusDevice;

public class FeneconPro extends ModbusDevice {
	@IsDeviceNature
	public FeneconProEss ess = null;

	@IsDeviceNature
	public FeneconProPvMeter pvMeter = null;

	@IsDeviceNature
	public FeneconProNapMeter napMeter = null;

	public FeneconPro() throws OpenemsException {
		super();
	}

	@IsConfig("ess")
	public void setEss(FeneconProEss ess) {
		this.ess = ess;
	}

	@IsConfig("pvMeter")
	public void setPvMeter(FeneconProPvMeter meter) {
		this.pvMeter = meter;
	}

	@IsConfig("napMeter")
	public void setNapMeter(FeneconProNapMeter meter) {
		this.napMeter = meter;
	}

	@Override
	public String toString() {
		return "FeneconPro []";
	}
}
