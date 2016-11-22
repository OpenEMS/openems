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
package io.openems.impl.controller.symmetric.powerbyfrequency;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

/*
 * this Controller calculates the power consumption of the house and charges or discharges the storages to reach zero power consumption from the grid
 */
public class PowerByFrequencyController extends Controller {
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this, Meter.class);

	@Override public void run() {
		try {
			Ess ess = this.ess.value();
			Meter meter = this.meter.value();
			// Calculate required sum values
			long activePower = 0L;
			long reactivePower = 0L;
			if (meter.frequency.value() >= 49990 && meter.frequency.value() <= 50010) {
				// charge if SOC isn't in the expected range
				if (ess.soc.value() > 65 || ess.soc.value() < 30) {
					activePower = (long) (ess.maxNominalPower.value() * (300 - 0.006 * meter.frequency.value()));
				}
			} else {
				// calculate minimal Power for Frequency
				activePower = ess.maxNominalPower.value() * (250 - meter.frequency.value() / 200);
				if ((meter.frequency.value() > 50000 && ess.soc.value() > 65)
						|| (meter.frequency.value() < 50000 && ess.soc.value() < 35)) {
					// calculate maximal Power for frequency
					activePower = (long) (ess.maxNominalPower.value() * (300 - 0.006 * meter.frequency.value()));
				}
			}
			// reduce power to max Discharge
			if (activePower > 0 && activePower > ess.allowedDischarge.value()) {
				activePower = ess.allowedDischarge.value();
			} else if (activePower < 0 && activePower < ess.allowedCharge.value()) {
				activePower = ess.allowedCharge.value();
			}
			ess.setActivePower.pushWrite(activePower);
			ess.setReactivePower.pushWrite(reactivePower);
			log.info(ess.id() + " Set ActivePower [" + activePower + "], ReactivePower [" + reactivePower + "]");
		} catch (InvalidValueException | WriteChannelException e) {
			log.error(e.getMessage());
		}
	}

}
