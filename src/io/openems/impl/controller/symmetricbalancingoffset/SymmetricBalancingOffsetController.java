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
package io.openems.impl.controller.symmetricbalancingoffset;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.ControllerUtils;

/*
 * this Controller calculates the power consumption of the house and charges or discharges the storages to reach zero power consumption from the grid
 */
public class SymmetricBalancingOffsetController extends Controller {
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this, Meter.class);

	public final ConfigChannel<Integer> activePowerOffset = new ConfigChannel<>("activePowerOffset", this,
			Integer.class);
	public final ConfigChannel<Integer> reactivePowerOffset = new ConfigChannel<>("reactivePowerOffset", this,
			Integer.class);

	@Override public void run() {
		try {
			Ess ess = this.ess.value();
			// Calculate required sum values
			long calculatedPower = meter.value().activePower.value() + ess.activePower.value();
			long calculatedReactivePower = meter.value().reactivePower.value() + ess.reactivePower.value();
			long maxChargePower = ess.setActivePower.writeMin().orElse(ess.allowedCharge.value() * -1);
			long maxDischargePower = ess.setActivePower.writeMax().orElse(ess.allowedDischarge.value());
			System.out.println("ActivePower: " + calculatedPower + ", ReactivePower: " + calculatedReactivePower);
			calculatedPower -= activePowerOffset.value();
			calculatedReactivePower -= reactivePowerOffset.value();
			long activePower = ControllerUtils.reduceActivePower(calculatedPower, calculatedReactivePower,
					maxChargePower, maxDischargePower);
			long reactivePower = ControllerUtils.reduceReactivePower(calculatedPower, calculatedReactivePower,
					maxChargePower, maxDischargePower);
			ess.setActivePower.pushWrite(activePower);
			ess.setReactivePower.pushWrite(reactivePower);
			log.info(ess.id() + " Set ActivePower [" + activePower + "], ReactivePower [" + reactivePower + "]");
		} catch (InvalidValueException | WriteChannelException e) {
			log.error(e.getMessage());
		}
	}

}
