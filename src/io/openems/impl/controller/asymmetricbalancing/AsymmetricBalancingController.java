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
package io.openems.impl.controller.asymmetricbalancing;

import java.util.Collections;
import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMapping;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

/**
 * this Controller calculates the power consumption of the house and charges or discharges the storages to reach
 * zero/power consumption from the grid
 */

public class AsymmetricBalancingController extends Controller {

	/*
	 * Config
	 */
	private ConfigChannel<Double> cosPhi = new ConfigChannel<Double>("cosPhi", this, Double.class).defaultValue(0.95);

	@IsThingMapping public List<Ess> esss = null;

	@IsThingMapping public Meter meter;

	@Override public void run() {
		try {
			for (Ess ess : esss) {
				ess.setWorkState.pushWriteFromLabel(EssNature.START);
			}
			// Calculate required sum values
			long calculatedPower = meter.activePowerL1.value() + meter.activePowerL2.value()
					+ meter.activePowerL3.value();
			long calculatedPowerL1 = meter.activePowerL1.value();
			long calculatedPowerL2 = meter.activePowerL2.value();
			long calculatedPowerL3 = meter.activePowerL3.value();
			long maxChargePower = 0;
			long maxDischargePower = 0;
			long maxChargePowerL1 = 0;
			long maxDischargePowerL1 = 0;
			long maxChargePowerL2 = 0;
			long maxDischargePowerL2 = 0;
			long maxChargePowerL3 = 0;
			long maxDischargePowerL3 = 0;
			long useableSoc = 0;
			for (Ess ess : esss) {
				calculatedPower += ess.activePowerL1.value() + ess.activePowerL1.value() + ess.activePowerL3.value();
				calculatedPowerL1 += ess.activePowerL1.value();
				calculatedPowerL2 += ess.activePowerL2.value();
				calculatedPowerL3 += ess.activePowerL3.value();
				maxChargePower += ess.allowedCharge.value();
				maxDischargePower += ess.allowedDischarge.value();
				maxChargePowerL1 += ess.setActivePowerL1.writeMin().orElse(ess.allowedCharge.value() / 3);
				maxDischargePowerL1 += ess.setActivePowerL2.writeMax().orElse(ess.allowedDischarge.value() / 3);
				maxChargePowerL2 += ess.setActivePowerL2.writeMin().orElse(ess.allowedCharge.value() / 3);
				maxDischargePowerL2 += ess.setActivePowerL2.writeMax().orElse(ess.allowedDischarge.value() / 3);
				maxChargePowerL3 += ess.setActivePowerL3.writeMin().orElse(ess.allowedCharge.value() / 3);
				maxDischargePowerL3 += ess.setActivePowerL3.writeMax().orElse(ess.allowedDischarge.value() / 3);
				useableSoc += ess.useableSoc();
			}
			if (calculatedPower > 0) {
				/*
				 * Discharge
				 */
				if (calculatedPowerL1 > maxDischargePowerL1) {
					calculatedPowerL1 = maxDischargePowerL1;
				}
				if (calculatedPowerL2 > maxDischargePowerL2) {
					calculatedPowerL2 = maxDischargePowerL2;
				}
				if (calculatedPowerL3 > maxDischargePowerL3) {
					calculatedPowerL3 = maxDischargePowerL3;
				}
				// sort ess by useableSoc asc
				Collections.sort(esss, (a, b) -> {
					try {
						return (int) (a.useableSoc() - b.useableSoc());
					} catch (InvalidValueException e) {
						log.error(e.getMessage());
						return 0;
					}
				});
				for (int i = 0; i < esss.size(); i++) {
					Ess ess = esss.get(i);
					// calculate minimal power needed to fulfill the calculatedPower
					long minPowerL1 = calculatedPowerL1;
					long minPowerL2 = calculatedPowerL2;
					long minPowerL3 = calculatedPowerL3;
					for (int j = i + 1; j < esss.size(); j++) {
						if (esss.get(j).useableSoc() > 0) {
							minPowerL1 -= esss.get(j).allowedDischarge.value() / 3;
							minPowerL2 -= esss.get(j).allowedDischarge.value() / 3;
							minPowerL3 -= esss.get(j).allowedDischarge.value() / 3;
						}
					}
					if (minPowerL1 < 0) {
						minPowerL1 = 0;
					}
					if (minPowerL2 < 0) {
						minPowerL2 = 0;
					}
					if (minPowerL3 < 0) {
						minPowerL3 = 0;
					}
					// check maximal power to avoid larger charges then calculatedPower
					long maxPowerL1 = ess.allowedDischarge.value() / 3;
					long maxPowerL2 = ess.allowedDischarge.value() / 3;
					long maxPowerL3 = ess.allowedDischarge.value() / 3;
					if (calculatedPowerL1 < maxPowerL1) {
						maxPowerL1 = calculatedPowerL1;
					}
					if (calculatedPowerL2 < maxPowerL2) {
						maxPowerL2 = calculatedPowerL2;
					}
					if (calculatedPowerL3 < maxPowerL3) {
						maxPowerL3 = calculatedPowerL3;
					}
					double diffL1 = maxPowerL1 - minPowerL1;
					double diffL2 = maxPowerL2 - minPowerL2;
					double diffL3 = maxPowerL3 - minPowerL3;
					/*
					 * weight the range of possible power by the useableSoc
					 * if the useableSoc is negative the ess will be charged
					 */
					long powerL1 = (long) (Math.ceil(minPowerL1 + diffL1 / useableSoc * ess.useableSoc()));
					long powerL2 = (long) (Math.ceil(minPowerL2 + diffL2 / useableSoc * ess.useableSoc()));
					long powerL3 = (long) (Math.ceil(minPowerL3 + diffL3 / useableSoc * ess.useableSoc()));
					ess.setActivePowerL1.pushWrite(powerL1);
					ess.setActivePowerL2.pushWrite(powerL2);
					ess.setActivePowerL3.pushWrite(powerL3);
					ess.setReactivePowerL1.pushWrite(0L);
					ess.setReactivePowerL2.pushWrite(0L);
					ess.setReactivePowerL3.pushWrite(0L);
					log.info("Set ActivePowerL1 [" + powerL1 + "], ReactivePowerL1 [0],ActivePowerL2 [" + powerL2
							+ "], ReactivePowerL2 [0],ActivePowerL3 [" + powerL3 + "], ReactivePowerL3 [0]");
					calculatedPower -= powerL1 + powerL2 + powerL3;
					calculatedPowerL1 -= powerL1;
					calculatedPowerL2 -= powerL2;
					calculatedPowerL3 -= powerL3;
				}
			} else {
				/*
				 * Charge
				 */
				if (calculatedPowerL1 < maxChargePowerL1) {
					calculatedPowerL1 = maxChargePowerL1;
				}
				if (calculatedPowerL2 < maxChargePowerL2) {
					calculatedPowerL2 = maxChargePowerL2;
				}
				if (calculatedPowerL3 < maxChargePowerL3) {
					calculatedPowerL3 = maxChargePowerL3;
				}
				/*
				 * sort ess by 100 - useabelSoc
				 * 100 - 90 = 10
				 * 100 - 45 = 55
				 * 100 - (- 5) = 105
				 * => ess with negative useableSoc will be charged much more then one with positive useableSoc
				 */
				Collections.sort(esss, (a, b) -> {
					try {
						return (int) ((100 - a.useableSoc()) - (100 - b.useableSoc()));
					} catch (InvalidValueException e) {
						log.error(e.getMessage());
						return 0;
					}
				});
				for (int i = 0; i < esss.size(); i++) {
					Ess ess = esss.get(i);
					// calculate minimal power needed to fulfill the calculatedPower
					long minP = calculatedPower;
					long minPowerL1 = calculatedPowerL1;
					long minPowerL2 = calculatedPowerL2;
					long minPowerL3 = calculatedPowerL3;
					for (int j = i + 1; j < esss.size(); j++) {
						minP -= esss.get(j).allowedCharge.value();
						minPowerL1 -= esss.get(j).allowedCharge.value() / 3;
						minPowerL2 -= esss.get(j).allowedCharge.value() / 3;
						minPowerL3 -= esss.get(j).allowedCharge.value() / 3;
					}
					if (minP > 0) {
						minP = 0;
					}
					if (minPowerL1 > 0) {
						minPowerL1 = 0;
					}
					if (minPowerL2 > 0) {
						minPowerL2 = 0;
					}
					if (minPowerL3 > 0) {
						minPowerL3 = 0;
					}
					// check maximal power to avoid larger charges then calculatedPower
					long maxPowerL1 = ess.allowedCharge.value() / 3;
					long maxPowerL2 = ess.allowedCharge.value() / 3;
					long maxPowerL3 = ess.allowedCharge.value() / 3;
					if (calculatedPowerL1 > maxPowerL1) {
						maxPowerL1 = calculatedPowerL1;
					}
					if (calculatedPowerL2 > maxPowerL2) {
						maxPowerL2 = calculatedPowerL2;
					}
					if (calculatedPowerL3 > maxPowerL3) {
						maxPowerL3 = calculatedPowerL3;
					}
					double diffL1 = maxPowerL1 - minPowerL1;
					double diffL2 = maxPowerL2 - minPowerL2;
					double diffL3 = maxPowerL3 - minPowerL3;
					// weight the range of possible power by the useableSoc
					long powerL1 = (long) Math.floor(minPowerL1 + diffL1 / useableSoc * (100 - ess.useableSoc()));
					long powerL2 = (long) Math.floor(minPowerL2 + diffL2 / useableSoc * (100 - ess.useableSoc()));
					long powerL3 = (long) Math.floor(minPowerL3 + diffL3 / useableSoc * (100 - ess.useableSoc()));
					ess.setActivePowerL1.pushWrite(powerL1);
					ess.setActivePowerL2.pushWrite(powerL2);
					ess.setActivePowerL3.pushWrite(powerL3);
					calculatedPower -= powerL1 + powerL2 + powerL3;
					calculatedPowerL1 -= powerL1;
					calculatedPowerL2 -= powerL2;
					calculatedPowerL3 -= powerL3;
				}
			}

			// }
		} catch (InvalidValueException | WriteChannelException e) {
			log.error(e.getMessage());
		}
	}

}
