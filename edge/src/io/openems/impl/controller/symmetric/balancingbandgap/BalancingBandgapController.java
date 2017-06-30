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
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;

@ThingInfo(title = "Balancing bandgap (Symmetric)", description = "Tries to keep the grid meter within a bandgap. For symmetric Ess.")
public class BalancingBandgapController extends Controller {

	/*
	 * Constructors
	 */
	public BalancingBandgapController() {
		super();
	}

	public BalancingBandgapController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ConfigInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ConfigInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ConfigInfo(title = "Min-ActivePower", description = "Low boundary of active power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> minActivePower = new ConfigChannel<>("minActivePower", this);

	@ConfigInfo(title = "Max-ActivePower", description = "High boundary of active power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> maxActivePower = new ConfigChannel<>("maxActivePower", this);

	@ConfigInfo(title = "Min-ReactivePower", description = "Low boundary of reactive power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> minReactivePower = new ConfigChannel<>("minReactivePower", this);

	@ConfigInfo(title = "Max-ReactivePower", description = "High boundary of reactive power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> maxReactivePower = new ConfigChannel<>("maxReactivePower", this);

	@ConfigInfo(title = "Enable ActivePower", description = "Indicates if active power bandgap is enabled.", type = Boolean.class, defaultValue = "true")
	public final ConfigChannel<Boolean> activePowerActivated = new ConfigChannel<Boolean>("activePowerActivated", this);

	@ConfigInfo(title = "Enable ReactivePower", description = "Indicates if reactive power bandgap is enabled.", type = Boolean.class, defaultValue = "true")
	public final ConfigChannel<Boolean> reactivePowerActivated = new ConfigChannel<Boolean>("reactivePowerActivated",
			this);

	/*
	 * Methods
	 */
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
