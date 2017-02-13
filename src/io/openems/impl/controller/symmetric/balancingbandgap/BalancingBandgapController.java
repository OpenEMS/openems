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
package io.openems.impl.controller.symmetric.balancingbandgap;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;

/*
 * this Controller calculates the power consumption of the house and charges or discharges the storages to reach zero power consumption from the grid
 */
public class BalancingBandgapController extends Controller {
	@ConfigInfo(title = "The storage which should be controlled", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ConfigInfo(title = "The meter which meassures the power from/to the grid", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ConfigInfo(title = "Lower limit of the activepower bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> minActivePower = new ConfigChannel<>("minActivePower", this);
	@ConfigInfo(title = "Upper limit of the activepower bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> maxActivePower = new ConfigChannel<>("maxActivePower", this);
	@ConfigInfo(title = "Lower limit of the reactivepower bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> minReactivePower = new ConfigChannel<>("minReactivePower", this);
	@ConfigInfo(title = "Upper limit of the reactivepower bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> maxReactivePower = new ConfigChannel<>("maxReactivePower", this);
	@ConfigInfo(title = "sign if activepower bandgap is activated", type = Boolean.class)
	public final ConfigChannel<Boolean> activePowerActivated = new ConfigChannel<Boolean>("activePowerActivated", this)
			.defaultValue(true);
	@ConfigInfo(title = "sign if reactivepower bandgap is activated", type = Boolean.class)
	public final ConfigChannel<Boolean> reactivePowerActivated = new ConfigChannel<Boolean>("reactivePowerActivated",
			this).defaultValue(true);

	public BalancingBandgapController() {
		super();
	}

	public BalancingBandgapController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			Meter meter = this.meter.value();
			// Calculate required sum values
			long calculatedPower = meter.activePower.value() + ess.activePower.value();
			long calculatedReactivePower = meter.reactivePower.value() + ess.reactivePower.value();
			if (calculatedPower >= maxActivePower.value()) {
				calculatedPower -= maxActivePower.value();
			} else if (calculatedPower <= minActivePower.value()) {
				calculatedPower -= minActivePower.value();
			} else {
				calculatedPower = 0;
			}
			if (calculatedReactivePower >= maxReactivePower.value()) {
				calculatedReactivePower -= maxReactivePower.value();
			} else if (calculatedReactivePower <= minReactivePower.value()) {
				calculatedReactivePower -= minReactivePower.value();
			} else {
				calculatedReactivePower = 0;
			}
			if (reactivePowerActivated.value()) {
				ess.power.setReactivePower(calculatedReactivePower);
			}
			if (activePowerActivated.value()) {
				ess.power.setActivePower(calculatedPower);
			}
			ess.power.writePower();
			// write info message to log
			String message = ess.id();
			if (activePowerActivated.value()) {
				message = message + " Set ActivePower [" + ess.power.getActivePower() + "]";
			}
			if (reactivePowerActivated.value()) {
				message = message + " Set ReactivePower [" + ess.power.getReactivePower() + "]";
			}
			log.info(message);
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}
	}

}
