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
package io.openems.impl.controller.balancingThreePhase;

import java.util.Collections;
import java.util.List;

import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMapping;
import io.openems.api.device.nature.EssNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

/*
 * this Controller calculates the power consumption of the house and charges or discharges the storages to reach zero power consumption from the grid
 */
public class BalancingThreePhaseController extends Controller {
	@IsThingMapping
	public List<Ess> esss = null;

	@IsThingMapping
	public Meter meter;

	private double cosPhi = 0.95;

	@Override
	public void run() {
		try {
			for (Ess ess : esss) {
				ess.setWorkState.pushWriteValue(EssNature.START);
			}
			// Calculate required sum values
			long calculatedPower = meter.activePowerPhaseA.getValue() + meter.activePowerPhaseB.getValue()
					+ meter.activePowerPhaseC.getValue();
			long calculatedPowerPhaseA = meter.activePowerPhaseA.getValue();
			long calculatedPowerPhaseB = meter.activePowerPhaseB.getValue();
			long calculatedPowerPhaseC = meter.activePowerPhaseC.getValue();
			long maxChargePower = 0;
			long maxDischargePower = 0;
			long maxChargePowerPhaseA = 0;
			long maxDischargePowerPhaseA = 0;
			long maxChargePowerPhaseB = 0;
			long maxDischargePowerPhaseB = 0;
			long maxChargePowerPhaseC = 0;
			long maxDischargePowerPhaseC = 0;
			long useableSoc = 0;
			for (Ess ess : esss) {
				calculatedPower += ess.activePowerPhaseA.getValue() + ess.activePowerPhaseB.getValue()
						+ ess.activePowerPhaseC.getValue();
				calculatedPowerPhaseA += ess.activePowerPhaseA.getValue();
				calculatedPowerPhaseB += ess.activePowerPhaseB.getValue();
				calculatedPowerPhaseC += ess.activePowerPhaseC.getValue();
				maxChargePower += ess.allowedCharge.getValue();
				maxDischargePower += ess.allowedDischarge.getValue();
				maxChargePowerPhaseA += ess.setActivePowerPhaseA.getMinValueOptional()
						.orElse(ess.allowedCharge.getValue() / 3);
				maxDischargePowerPhaseA += ess.setActivePowerPhaseA.getMaxValueOptional()
						.orElse(ess.allowedDischarge.getValue() / 3);
				maxChargePowerPhaseB += ess.setActivePowerPhaseB.getMinValueOptional()
						.orElse(ess.allowedCharge.getValue() / 3);
				maxDischargePowerPhaseB += ess.setActivePowerPhaseB.getMaxValueOptional()
						.orElse(ess.allowedDischarge.getValue() / 3);
				maxChargePowerPhaseC += ess.setActivePowerPhaseC.getMinValueOptional()
						.orElse(ess.allowedCharge.getValue() / 3);
				maxDischargePowerPhaseC += ess.setActivePowerPhaseC.getMaxValueOptional()
						.orElse(ess.allowedDischarge.getValue() / 3);
				useableSoc += ess.useableSoc();
			}
			if (calculatedPower > 0) {
				/*
				 * Discharge
				 */
				if (calculatedPowerPhaseA > maxDischargePowerPhaseA) {
					calculatedPowerPhaseA = maxDischargePowerPhaseA;
				}
				if (calculatedPowerPhaseB > maxDischargePowerPhaseB) {
					calculatedPowerPhaseB = maxDischargePowerPhaseB;
				}
				if (calculatedPowerPhaseC > maxDischargePowerPhaseC) {
					calculatedPowerPhaseC = maxDischargePowerPhaseC;
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
					long minPowerPhaseA = calculatedPowerPhaseA;
					long minPowerPhaseB = calculatedPowerPhaseB;
					long minPowerPhaseC = calculatedPowerPhaseC;
					for (int j = i + 1; j < esss.size(); j++) {
						if (esss.get(j).useableSoc() > 0) {
							minPowerPhaseA -= esss.get(j).allowedDischarge.getValue() / 3;
							minPowerPhaseB -= esss.get(j).allowedDischarge.getValue() / 3;
							minPowerPhaseC -= esss.get(j).allowedDischarge.getValue() / 3;
						}
					}
					if (minPowerPhaseA < 0) {
						minPowerPhaseA = 0;
					}
					if (minPowerPhaseB < 0) {
						minPowerPhaseB = 0;
					}
					if (minPowerPhaseC < 0) {
						minPowerPhaseC = 0;
					}
					// check maximal power to avoid larger charges then calculatedPower
					long maxPowerPhaseA = ess.allowedDischarge.getValue() / 3;
					long maxPowerPhaseB = ess.allowedDischarge.getValue() / 3;
					long maxPowerPhaseC = ess.allowedDischarge.getValue() / 3;
					if (calculatedPowerPhaseA < maxPowerPhaseA) {
						maxPowerPhaseA = calculatedPowerPhaseA;
					}
					if (calculatedPowerPhaseB < maxPowerPhaseB) {
						maxPowerPhaseB = calculatedPowerPhaseB;
					}
					if (calculatedPowerPhaseC < maxPowerPhaseC) {
						maxPowerPhaseC = calculatedPowerPhaseC;
					}
					double diffPhaseA = maxPowerPhaseA - minPowerPhaseA;
					double diffPhaseB = maxPowerPhaseB - minPowerPhaseB;
					double diffPhaseC = maxPowerPhaseC - minPowerPhaseC;
					/*
					 * weight the range of possible power by the useableSoc
					 * if the useableSoc is negative the ess will be charged
					 */
					long powerPhaseA = (long) (Math.ceil(minPowerPhaseA + diffPhaseA / useableSoc * ess.useableSoc()));
					long powerPhaseB = (long) (Math.ceil(minPowerPhaseB + diffPhaseB / useableSoc * ess.useableSoc()));
					long powerPhaseC = (long) (Math.ceil(minPowerPhaseC + diffPhaseC / useableSoc * ess.useableSoc()));
					ess.setActivePowerPhaseA.pushWriteValue(powerPhaseA);
					ess.setActivePowerPhaseB.pushWriteValue(powerPhaseB);
					ess.setActivePowerPhaseC.pushWriteValue(powerPhaseC);
					ess.setReactivePowerPhaseA.pushWriteValue(0);
					ess.setReactivePowerPhaseB.pushWriteValue(0);
					ess.setReactivePowerPhaseC.pushWriteValue(0);
					log.info("Set ActivePowerPhase1 [" + powerPhaseA + "], ReactivePowerPhase1 [0],ActivePowerPhase2 ["
							+ powerPhaseB + "], ReactivePowerPhase2 [0],ActivePowerPhase3 [" + powerPhaseC
							+ "], ReactivePowerPhase3 [0]");
					calculatedPower -= powerPhaseA + powerPhaseB + powerPhaseC;
					calculatedPowerPhaseA -= powerPhaseA;
					calculatedPowerPhaseB -= powerPhaseB;
					calculatedPowerPhaseC -= powerPhaseC;
				}
			} else {
				/*
				 * Charge
				 */
				if (calculatedPowerPhaseA < maxChargePowerPhaseA) {
					calculatedPowerPhaseA = maxChargePowerPhaseA;
				}
				if (calculatedPowerPhaseB < maxChargePowerPhaseB) {
					calculatedPowerPhaseB = maxChargePowerPhaseB;
				}
				if (calculatedPowerPhaseC < maxChargePowerPhaseC) {
					calculatedPowerPhaseC = maxChargePowerPhaseC;
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
					long minPowerPhaseA = calculatedPowerPhaseA;
					long minPowerPhaseB = calculatedPowerPhaseB;
					long minPowerPhaseC = calculatedPowerPhaseC;
					for (int j = i + 1; j < esss.size(); j++) {
						minP -= esss.get(j).allowedCharge.getValue();
						minPowerPhaseA -= esss.get(j).allowedCharge.getValue() / 3;
						minPowerPhaseB -= esss.get(j).allowedCharge.getValue() / 3;
						minPowerPhaseC -= esss.get(j).allowedCharge.getValue() / 3;
					}
					if (minP > 0) {
						minP = 0;
					}
					if (minPowerPhaseA > 0) {
						minPowerPhaseA = 0;
					}
					if (minPowerPhaseB > 0) {
						minPowerPhaseB = 0;
					}
					if (minPowerPhaseC > 0) {
						minPowerPhaseC = 0;
					}
					// check maximal power to avoid larger charges then calculatedPower
					long maxPowerPhaseA = ess.allowedCharge.getValue() / 3;
					long maxPowerPhaseB = ess.allowedCharge.getValue() / 3;
					long maxPowerPhaseC = ess.allowedCharge.getValue() / 3;
					if (calculatedPowerPhaseA > maxPowerPhaseA) {
						maxPowerPhaseA = calculatedPowerPhaseA;
					}
					if (calculatedPowerPhaseB > maxPowerPhaseB) {
						maxPowerPhaseB = calculatedPowerPhaseB;
					}
					if (calculatedPowerPhaseC > maxPowerPhaseC) {
						maxPowerPhaseC = calculatedPowerPhaseC;
					}
					double diffPhaseA = maxPowerPhaseA - minPowerPhaseA;
					double diffPhaseB = maxPowerPhaseB - minPowerPhaseB;
					double diffPhaseC = maxPowerPhaseC - minPowerPhaseC;
					// weight the range of possible power by the useableSoc
					long powerPhaseA = (long) Math
							.floor(minPowerPhaseA + diffPhaseA / useableSoc * (100 - ess.useableSoc()));
					long powerPhaseB = (long) Math
							.floor(minPowerPhaseB + diffPhaseB / useableSoc * (100 - ess.useableSoc()));
					long powerPhaseC = (long) Math
							.floor(minPowerPhaseC + diffPhaseC / useableSoc * (100 - ess.useableSoc()));
					ess.setActivePowerPhaseA.pushWriteValue(powerPhaseA);
					ess.setActivePowerPhaseB.pushWriteValue(powerPhaseB);
					ess.setActivePowerPhaseC.pushWriteValue(powerPhaseC);
					calculatedPower -= powerPhaseA + powerPhaseB + powerPhaseC;
					calculatedPowerPhaseA -= powerPhaseA;
					calculatedPowerPhaseB -= powerPhaseB;
					calculatedPowerPhaseC -= powerPhaseC;
				}
			}

			// }
		} catch (InvalidValueException | WriteChannelException e) {
			log.error(e.getMessage());
		}
	}

}
