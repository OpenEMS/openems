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
package io.openems.impl.controller.symmetricbalancingbandgap;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

/*
 * this Controller calculates the power consumption of the house and charges or discharges the storages to reach zero power consumption from the grid
 */
public class SymmetricBalancingBandgapController extends Controller {
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this, Meter.class);

	public final ConfigChannel<Integer> minActivePower = new ConfigChannel<>("minActivePower", this, Integer.class);
	public final ConfigChannel<Integer> maxActivePower = new ConfigChannel<>("maxActivePower", this, Integer.class);
	public final ConfigChannel<Integer> minReactivePower = new ConfigChannel<>("minReactivePower", this, Integer.class);
	public final ConfigChannel<Integer> maxReactivePower = new ConfigChannel<>("maxReactivePower", this, Integer.class);

	@Override public void run() {
		try {
			Ess ess = this.ess.value();
			// Calculate required sum values
			long calculatedPower = meter.value().activePower.value() + ess.activePower.value();
			long calculatedReactivePower = meter.value().reactivePower.value() + ess.reactivePower.value();
			long maxChargePower = ess.setActivePower.writeMin().orElse(0L);
			long maxDischargePower = ess.setActivePower.writeMax().orElse(0L);
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
			if (calculatedPower > 0) {
				/*
				 * Discharge
				 */
				if (calculateApparentPower(calculatedPower, calculatedReactivePower) > maxDischargePower) {
					double cosPhi = calculateCosPhi(calculatedPower, calculatedReactivePower);
					calculatedPower = calculateActivePower(maxDischargePower, cosPhi);
					calculatedReactivePower = calculateReactivePower(maxDischargePower, cosPhi);
				}
			} else {
				/*
				 * Charge
				 */
				if (calculateApparentPower(calculatedPower, calculatedReactivePower) < maxChargePower) {
					double cosPhi = calculateCosPhi(calculatedPower, calculatedReactivePower);
					calculatedPower = calculateActivePower(maxChargePower, cosPhi);
					calculatedReactivePower = calculateReactivePower(maxChargePower, cosPhi);
				}

			}
			ess.setActivePower.pushWrite(calculatedPower);
			ess.setReactivePower.pushWrite(calculatedReactivePower);
			log.info(ess.id() + " Set ActivePower [" + calculatedPower + "], ReactivePower [" + calculatedReactivePower
					+ "]");
		} catch (InvalidValueException | WriteChannelException e) {
			log.error(e.getMessage());
		}
	}

	private double calculateCosPhi(long activePower, long reactivePower) {
		return activePower / calculateApparentPower(activePower, reactivePower);
	}

	private long calculateReactivePower(long activePower, double cosPhi) {
		return (long) (activePower * Math.sqrt(1 / Math.pow(cosPhi, 2) - 1));
	}

	private long calculateApparentPower(long activePower, long reactivePower) {
		return (long) Math.sqrt(Math.pow(activePower, 2) + Math.pow(reactivePower, 2));
	}

	private long calculateActivePower(long apparentPower, double cosPhi) {
		return (long) (apparentPower * cosPhi);
	}

	private long calculateApparentPower(long activePower, double cosPhi) {
		return (long) (activePower / cosPhi);
	}

}
