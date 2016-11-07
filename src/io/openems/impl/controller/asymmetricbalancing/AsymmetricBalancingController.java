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

	@IsThingMapping public ConfigChannel<List<Ess>> esss = new ConfigChannel<>("esss", this, Ess.class);

	@IsThingMapping public ConfigChannel<Meter> meter = new ConfigChannel<>("meters", this, Meter.class);

	private long calculatedPower = 0L;
	private long calculatedPowerL1 = 0L;
	private long calculatedPowerL2 = 0L;
	private long calculatedPowerL3 = 0L;
	private long maxChargePower = 0L;
	private long maxDischargePower = 0L;
	private double percentageL1 = 0;
	private double percentageL2 = 0;
	private double percentageL3 = 0;

	@Override public void run() {
		try {
			for (Ess ess : esss.value()) {
				ess.setWorkState.pushWriteFromLabel(EssNature.START);
			}
			calculateRequiredPower();
			calculateAllowedPower();
			reducePowerToPossiblePower();
			calculateMinMaxValues();
			// Calculate required sum values
			long maxChargePowerL1 = 0;
			long maxDischargePowerL1 = 0;
			long maxChargePowerL2 = 0;
			long maxDischargePowerL2 = 0;
			long maxChargePowerL3 = 0;
			long maxDischargePowerL3 = 0;
			long useableSoc = 0;
			for (Ess ess : esss.value()) {
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
				Collections.sort(esss.value(), (a, b) -> {
					try {
						return (int) (a.useableSoc() - b.useableSoc());
					} catch (InvalidValueException e) {
						log.error(e.getMessage());
						return 0;
					}
				});
				for (int i = 0; i < esss.value().size(); i++) {
					Ess ess = esss.value().get(i);
					// calculate minimal power needed to fulfill the calculatedPower
					long minPowerL1 = calculatedPowerL1;
					long minPowerL2 = calculatedPowerL2;
					long minPowerL3 = calculatedPowerL3;
					for (int j = i + 1; j < esss.value().size(); j++) {
						if (esss.value().get(j).useableSoc() > 0) {
							minPowerL1 -= esss.value().get(j).allowedDischarge.value() / 3;
							minPowerL2 -= esss.value().get(j).allowedDischarge.value() / 3;
							minPowerL3 -= esss.value().get(j).allowedDischarge.value() / 3;
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
				Collections.sort(esss.value(), (a, b) -> {
					try {
						return (int) ((100 - a.useableSoc()) - (100 - b.useableSoc()));
					} catch (InvalidValueException e) {
						log.error(e.getMessage());
						return 0;
					}
				});
				for (int i = 0; i < esss.value().size(); i++) {
					Ess ess = esss.value().get(i);
					// calculate minimal power needed to fulfill the calculatedPower
					long minP = calculatedPower;
					long minPowerL1 = calculatedPowerL1;
					long minPowerL2 = calculatedPowerL2;
					long minPowerL3 = calculatedPowerL3;
					for (int j = i + 1; j < esss.value().size(); j++) {
						minP -= esss.value().get(j).allowedCharge.value();
						minPowerL1 -= esss.value().get(j).allowedCharge.value() / 3;
						minPowerL2 -= esss.value().get(j).allowedCharge.value() / 3;
						minPowerL3 -= esss.value().get(j).allowedCharge.value() / 3;
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

	private void calculateMinMaxValues() throws WriteChannelException, InvalidValueException {
		for (Ess ess : esss.value()) {
			long maxPowerL1 = 0;
			long maxPowerL2 = 0;
			long maxPowerL3 = 0;
			long minPowerL1 = 0;
			long minPowerL2 = 0;
			long minPowerL3 = 0;
			if (ess.allowedApparent.value() < ess.allowedDischarge.value()) {
				maxPowerL1 = calculateActivePower((long) (ess.allowedApparent.value() * percentageL1), cosPhi.value());
				maxPowerL2 = calculateActivePower((long) (ess.allowedApparent.value() * percentageL2), cosPhi.value());
				maxPowerL3 = calculateActivePower((long) (ess.allowedApparent.value() * percentageL3), cosPhi.value());
			} else {
				maxPowerL1 = calculateActivePower((long) (ess.allowedDischarge.value() * percentageL1), cosPhi.value());
				maxPowerL2 = calculateActivePower((long) (ess.allowedDischarge.value() * percentageL2), cosPhi.value());
				maxPowerL3 = calculateActivePower((long) (ess.allowedDischarge.value() * percentageL3), cosPhi.value());
			}
			if (ess.allowedApparent.value() < ess.allowedCharge.value()) {
				minPowerL1 = calculateActivePower((long) (ess.allowedApparent.value() * percentageL1), cosPhi.value());
				minPowerL2 = calculateActivePower((long) (ess.allowedApparent.value() * percentageL2), cosPhi.value());
				minPowerL3 = calculateActivePower((long) (ess.allowedApparent.value() * percentageL3), cosPhi.value());
			} else {
				minPowerL1 = calculateActivePower((long) (ess.allowedCharge.value() * percentageL1), cosPhi.value());
				minPowerL2 = calculateActivePower((long) (ess.allowedCharge.value() * percentageL2), cosPhi.value());
				minPowerL3 = calculateActivePower((long) (ess.allowedCharge.value() * percentageL3), cosPhi.value());
			}
			ess.setActivePowerL1.pushWriteMax(maxPowerL1);
			ess.setActivePowerL2.pushWriteMax(maxPowerL2);
			ess.setActivePowerL3.pushWriteMax(maxPowerL3);
			ess.setActivePowerL1.pushWriteMax(minPowerL1);
			ess.setActivePowerL2.pushWriteMax(minPowerL2);
			ess.setActivePowerL3.pushWriteMax(minPowerL3);
		}
	}

	private void calculateRequiredPower() throws InvalidValueException {
		Meter meter = this.meter.value();
		this.calculatedPower = meter.activePowerL1.value() + meter.activePowerL2.value() + meter.activePowerL3.value();
		this.calculatedPowerL1 = meter.activePowerL1.value();
		this.calculatedPowerL2 = meter.activePowerL2.value();
		this.calculatedPowerL3 = meter.activePowerL3.value();
		for (Ess ess : esss.value()) {
			this.calculatedPower += meter.activePowerL1.value() + meter.activePowerL2.value()
					+ meter.activePowerL3.value();
			this.calculatedPowerL1 += meter.activePowerL1.value();
			this.calculatedPowerL2 += meter.activePowerL2.value();
			this.calculatedPowerL3 += meter.activePowerL3.value();
		}
	}

	private void calculateAllowedPower() throws InvalidValueException {
		long allowedApparent = 0;
		long allowedCharge = 0;
		long allowedDischarge = 0;
		for (Ess ess : esss.value()) {
			allowedCharge += ess.allowedCharge.value();
			allowedDischarge += ess.allowedDischarge.value();
			allowedApparent += ess.allowedApparent.value();
		}
		this.maxChargePower = allowedApparent;
		if (allowedCharge < allowedApparent) {
			this.maxChargePower = allowedCharge;
		}
		this.maxDischargePower = allowedApparent;
		if (allowedDischarge < allowedApparent) {
			this.maxDischargePower = allowedDischarge;
		}
		this.maxChargePower *= -1;
	}

	private void reducePowerToPossiblePower() throws InvalidValueException {
		percentageL1 = (double) this.calculatedPowerL1 / (double) this.calculatedPower;
		percentageL2 = (double) this.calculatedPowerL2 / (double) this.calculatedPower;
		percentageL3 = (double) this.calculatedPowerL3 / (double) this.calculatedPower;
		if (calculateApparentPower(this.calculatedPower, cosPhi.value()) > maxDischargePower) {
			this.calculatedPower = calculateActivePower(maxDischargePower, cosPhi.value());
			this.calculatedPowerL1 = (long) (this.calculatedPower * percentageL1);
			this.calculatedPowerL2 = (long) (this.calculatedPower * percentageL2);
			this.calculatedPowerL3 = (long) (this.calculatedPower * percentageL3);
		} else if (calculateApparentPower(this.calculatedPower, cosPhi.value()) < maxChargePower) {
			this.calculatedPower = calculateActivePower(maxChargePower, cosPhi.value());
			this.calculatedPowerL1 = (long) (this.calculatedPower * percentageL1);
			this.calculatedPowerL2 = (long) (this.calculatedPower * percentageL2);
			this.calculatedPowerL3 = (long) (this.calculatedPower * percentageL3);
		}
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
