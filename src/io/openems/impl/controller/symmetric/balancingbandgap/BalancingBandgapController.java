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
package io.openems.impl.controller.symmetric.balancingbandgap;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.AvgFiFoQueue;
import io.openems.core.utilities.ControllerUtils;

/*
 * this Controller calculates the power consumption of the house and charges or discharges the storages to reach zero power consumption from the grid
 */
public class BalancingBandgapController extends Controller {
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this, Meter.class);

	public final ConfigChannel<Integer> minActivePower = new ConfigChannel<>("minActivePower", this, Integer.class);
	public final ConfigChannel<Integer> maxActivePower = new ConfigChannel<>("maxActivePower", this, Integer.class);
	public final ConfigChannel<Integer> minReactivePower = new ConfigChannel<>("minReactivePower", this, Integer.class);
	public final ConfigChannel<Integer> maxReactivePower = new ConfigChannel<>("maxReactivePower", this, Integer.class);
	public final ConfigChannel<Boolean> activePowerActivated = new ConfigChannel<Boolean>("activePowerActivated", this,
			Boolean.class).defaultValue(true);
	public final ConfigChannel<Boolean> reactivePowerActivated = new ConfigChannel<Boolean>("reactivePowerActivated",
			this, Boolean.class).defaultValue(true);

	private final AvgFiFoQueue activePowerQueue = new AvgFiFoQueue(10);
	private final AvgFiFoQueue reactivePowerQueue = new AvgFiFoQueue(10);

	@Override public void run() {
		try {
			Ess ess = this.ess.value();
			// Calculate required sum values
			long calculatedPower = meter.value().activePower.value() + ess.activePower.value();
			long calculatedReactivePower = meter.value().reactivePower.value() + ess.reactivePower.value();
			long maxChargePower = ess.setActivePower.writeMin().orElse(ess.allowedCharge.value() * -1);
			long maxDischargePower = ess.setActivePower.writeMax().orElse(ess.allowedDischarge.value());
			activePowerQueue.add(calculatedPower);
			reactivePowerQueue.add(calculatedReactivePower);
			calculatedPower = activePowerQueue.avg();
			calculatedReactivePower = reactivePowerQueue.avg();
			System.out.println("ActivePower: " + calculatedPower + ", ReactivePower: " + calculatedReactivePower);
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
			if (!reactivePowerActivated.value()) {
				calculatedReactivePower = 0;
			}
			long activePower = ControllerUtils.reduceActivePower(calculatedPower, calculatedReactivePower,
					maxChargePower, maxDischargePower);
			if (!activePowerActivated.value()) {
				calculatedPower = 0;
			}
			long reactivePower = ControllerUtils.reduceReactivePower(calculatedPower, calculatedReactivePower,
					maxChargePower, maxDischargePower);
			String message = ess.id();
			if (activePowerActivated.value()) {
				ess.setActivePower.pushWrite(activePower);
				message = message + " Set ActivePower [" + activePower + "]";
			}
			if (reactivePowerActivated.value()) {
				ess.setReactivePower.pushWrite(reactivePower);
				message = message + " Set ReactivePower [" + reactivePower + "]";
			}
			log.info(message);
		} catch (InvalidValueException | WriteChannelException e) {
			log.error(e.getMessage());
		}
	}

}
