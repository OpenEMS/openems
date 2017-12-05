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
package io.openems.impl.controller.asymmetric.balancing;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.ControllerUtils;

@ThingInfo(title = "Self-consumption optimization (Asymmetric)", description = "Tries to keep the grid meter on zero. For asymmetric Ess.")
public class BalancingController extends Controller {

	/*
	 * Constructors
	 */
	public BalancingController() {
		super();
	}

	public BalancingController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Cos-Phi", type = Double.class, defaultValue = "0.95")
	public ConfigChannel<Double> cosPhi = new ConfigChannel<Double>("cosPhi", this);

	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	/*
	 * Fields
	 */
	private long[][] lastWriteValues = new long[3][5];
	private int index = 0;

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			for (Ess ess : esss.value()) {
				ess.setWorkState.pushWriteFromLabel(EssNature.START);
			}
			long[] calculatedPowers = new long[3];
			long calculatedPowerSum = 0;
			// calculateRequiredPower
			Meter meter = this.meter.value();
			calculatedPowers[0] = meter.activePowerL1.value();
			calculatedPowerSum += meter.activePowerL1.value();
			calculatedPowers[1] = meter.activePowerL2.value();
			calculatedPowerSum += meter.activePowerL2.value();
			calculatedPowers[2] = meter.activePowerL3.value();
			calculatedPowerSum += meter.activePowerL3.value();
			for (Ess ess : esss.value()) {
				calculatedPowers[0] += ess.activePowerL1.value();
				calculatedPowerSum += ess.activePowerL1.value();
				calculatedPowers[1] += ess.activePowerL2.value();
				calculatedPowerSum += ess.activePowerL2.value();
				calculatedPowers[2] += ess.activePowerL3.value();
				calculatedPowerSum += ess.activePowerL3.value();
			}
			if (Math.abs(calculatedPowerSum) > 100) {
				for (int i = 0; i < 3; i++) {
					lastWriteValues[i][index] = calculatedPowers[i];
					calculatedPowers[i] = getAvgPower(i + 1);
				}
				index++;
				index %= lastWriteValues[0].length;
				// Calculate required sum values
				long useableSoc = 0;
				for (Ess ess : esss.value()) {
					useableSoc += ess.useableSoc();
				}
				// Loop each Phase
				for (int i = 1; i <= 3; i++) {
					long absolutePower = Math.abs(calculatedPowers[0]) + Math.abs(calculatedPowers[1])
					+ Math.abs(calculatedPowers[2]);
					double percentage = (double) calculatedPowers[i - 1] / absolutePower;
					long maxChargePowerPhase = 0L;
					long maxDischargePowerPhase = 0L;
					for (Ess ess : esss.value()) {
						Tupel<Long> minMax = calculateMinMaxValues(ess, percentage, cosPhi.value(), i);
						maxDischargePowerPhase += minMax.b;
						maxChargePowerPhase += minMax.a;
						try {
							ess.getSetActivePower(i).pushWriteMax(minMax.b);
						} catch (WriteChannelException e) {
							log.debug(e.getMessage());
						}
						try {
							ess.getSetActivePower(i).pushWriteMin(minMax.a);
						} catch (WriteChannelException e) {
							log.debug(e.getMessage());
						}
					}
					// reduce Power to possible power
					if (calculatedPowers[i - 1] > maxDischargePowerPhase) {
						calculatedPowers[i - 1] = maxDischargePowerPhase;
					} else if (calculatedPowers[i - 1] < maxChargePowerPhase) {
						calculatedPowers[i - 1] = maxChargePowerPhase;
					}
					calculatePower(calculatedPowers[i - 1], maxDischargePowerPhase, maxChargePowerPhase, i, useableSoc);
				}
			}
			for (Ess ess : esss.value()) {
				log.debug(ess.getSetValueLog());
			}
		} catch (InvalidValueException | WriteChannelException e) {
			log.error(e.getMessage());
		}
	}

	private long getAvgPower(int phase) {
		int i = index;
		long sum = 0;
		do {
			sum += lastWriteValues[phase - 1][i];
			i++;
			i %= lastWriteValues[phase - 1].length;
		} while (i != index);
		return sum / lastWriteValues[phase - 1].length;
	}

	/**
	 * calculates active and reactive power for a phase and set the calculated values to the ess
	 *
	 * @param calculatedPower
	 * @param maxDischargePower
	 * @param maxChargePower
	 * @param phase
	 * @param useableSoc
	 * @throws InvalidValueException
	 * @throws WriteChannelException
	 */
	private void calculatePower(long calculatedPower, long maxDischargePower, long maxChargePower, int phase,
			long useableSoc) throws InvalidValueException, WriteChannelException {
		if (calculatedPower >= 0) {
			/*
			 * Discharge
			 */
			if (calculatedPower > maxDischargePower) {
				calculatedPower = maxDischargePower;
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
		} else {
			/*
			 * Charge
			 */
			if (calculatedPower < maxChargePower) {
				calculatedPower = maxChargePower;
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
		}

		for (int i = 0; i < esss.value().size(); i++) {
			Ess ess = esss.value().get(i);
			// calculate minimal power needed to fulfill the calculatedPower
			long minPower = 0;
			long maxPower = 0;
			long power = 0;
			if (calculatedPower >= 0) {
				/*
				 * Discharge
				 */
				minPower = calculatedPower;
				for (int j = i + 1; j < esss.value().size(); j++) {
					if (esss.value().get(j).useableSoc() > 0) {
						minPower -= esss.value().get(j).getSetActivePower(phase).writeMax()
								.orElse(esss.value().get(j).allowedDischarge.value() / 3);
					}
				}
				if (minPower < 0) {
					minPower = 0;
				}
				// check maximal power to avoid larger charges then calculatedPower
				maxPower = ess.getSetActivePower(phase).writeMax().orElse(ess.allowedCharge.value() / 3);
				if (calculatedPower < maxPower) {
					maxPower = calculatedPower;
				}
				double diff = maxPower - minPower;
				/*
				 * weight the range of possible power by the useableSoc
				 * if the useableSoc is negative the ess will be charged
				 */
				power = (long) (Math.ceil(minPower + diff / useableSoc * ess.useableSoc()));
			} else {
				/*
				 * Charge
				 */
				minPower = calculatedPower;
				for (int j = i + 1; j < esss.value().size(); j++) {
					minPower -= esss.value().get(j).getSetActivePower(phase).writeMin()
							.orElse(esss.value().get(j).allowedCharge.value() / 3);
				}
				if (minPower > 0) {
					minPower = 0;
				}
				// check maximal power to avoid larger charges then calculatedPower
				maxPower = ess.getSetActivePower(phase).writeMin().orElse(ess.allowedCharge.value() / 3);
				if (calculatedPower > maxPower) {
					maxPower = calculatedPower;
				}
				double diff = maxPower - minPower;
				/*
				 * weight the range of possible power by the useableSoc
				 * if the useableSoc is negative the ess will be charged
				 */
				power = (long) (Math
						.ceil(minPower + diff / (esss.value().size() * 100 - useableSoc) * (100 - ess.useableSoc())));
			}

			long reactivePower = ControllerUtils.calculateReactivePower(power, cosPhi.value());

			calculatedPower -= power;

			ess.getSetActivePower(phase).pushWrite(power);
			ess.getSetReactivePower(phase).pushWrite(reactivePower);
		}
	}

	/**
	 * Calculates minimal and maximal value for an Phase
	 * with the reactivePower
	 *
	 * @param ess
	 * @param percentage
	 *            of the power of the phase in realation to the whole power
	 * @param cosPhi
	 * @return a Tupel with value a minPower and value b maxPower
	 * @throws InvalidValueException
	 */
	private Tupel<Long> calculateMinMaxValues(Ess ess, double percentage, double cosPhi, int phase)
			throws InvalidValueException {
		long maxPower = 0;
		long minPower = 0;
		percentage = Math.abs(percentage);

		Optional<Long> writeMin = ess.getSetActivePower(phase).writeMin();
		Optional<Long> writeMax = ess.getSetActivePower(phase).writeMax();

		maxPower = (long) ((double) ess.allowedDischarge.value() * percentage);

		if (writeMax.isPresent() && maxPower > writeMax.get()) {
			maxPower = writeMax.get();
		}

		minPower = (long) ((double) ess.allowedCharge.value() * percentage);

		if (writeMin.isPresent() && minPower < writeMin.get()) {
			minPower = writeMin.get();
		}

		if (ControllerUtils.calculateApparentPower(minPower, cosPhi) > ess.allowedApparent.value() / 3) {
			minPower = ControllerUtils.calculateActivePowerFromApparentPower(ess.allowedApparent.value() / 3, cosPhi)
					* -1;
		}
		if (ControllerUtils.calculateApparentPower(maxPower, cosPhi) > ess.allowedApparent.value() / 3) {
			maxPower = ControllerUtils.calculateActivePowerFromApparentPower(ess.allowedApparent.value() / 3, cosPhi);
		}
		return new Tupel<Long>(minPower, maxPower);
	}

	private class Tupel<T> {
		final T a;
		final T b;

		private Tupel(T a, T b) {
			this.a = a;
			this.b = b;
		}
	}

}
