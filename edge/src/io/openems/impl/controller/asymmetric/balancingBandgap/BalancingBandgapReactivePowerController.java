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
package io.openems.impl.controller.asymmetric.balancingBandgap;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.AsymmetricPower.ReductionType;
import io.openems.core.utilities.AvgFiFoQueue;

@ThingInfo(title = "Self-consumption optimization (Asymmetric)", description = "Tries to keep the grid meter on zero. For asymmetric Ess.")
public class BalancingBandgapReactivePowerController extends Controller {

	/*
	 * Constructors
	 */
	public BalancingBandgapReactivePowerController() {
		super();
	}

	public BalancingBandgapReactivePowerController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	// @ConfigInfo(title = "Cos-Phi", type = Double.class, defaultValue = "0.95")
	// public ConfigChannel<Double> cosPhi = new ConfigChannel<Double>("cosPhi", this);

	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public ConfigChannel<Ess> esss = new ConfigChannel<Ess>("esss", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ChannelInfo(title = "Max-RectivePowerL1", description = "High boundary of reactive power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> maxReactivePowerL1 = new ConfigChannel<>("maxReactivePowerL1", this);

	@ChannelInfo(title = "Min-ReactivePowerL1", description = "Low boundary of reactive power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> minReactivePowerL1 = new ConfigChannel<>("minReactivePowerL1", this);

	@ChannelInfo(title = "Max-RectivePowerL2", description = "High boundary of reactive power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> maxReactivePowerL2 = new ConfigChannel<>("maxReactivePowerL2", this);

	@ChannelInfo(title = "Min-ReactivePowerL2", description = "Low boundary of reactive power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> minReactivePowerL2 = new ConfigChannel<>("minReactivePowerL2", this);

	@ChannelInfo(title = "Max-RectivePowerL3", description = "High boundary of reactive power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> maxReactivePowerL3 = new ConfigChannel<>("maxReactivePowerL3", this);

	@ChannelInfo(title = "Min-ReactivePowerL3", description = "Low boundary of reactive power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> minReactivePowerL3 = new ConfigChannel<>("minReactivePowerL3", this);

	private AvgFiFoQueue meterL1 = new AvgFiFoQueue(2, 1.5);
	private AvgFiFoQueue meterL2 = new AvgFiFoQueue(2, 1.5);
	private AvgFiFoQueue meterL3 = new AvgFiFoQueue(2, 1.5);
	private AvgFiFoQueue essL1 = new AvgFiFoQueue(2, 1.5);
	private AvgFiFoQueue essL2 = new AvgFiFoQueue(2, 1.5);
	private AvgFiFoQueue essL3 = new AvgFiFoQueue(2, 1.5);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			long[] calculatedPowers = new long[3];
			// calculateRequiredPower
			Meter meter = this.meter.value();
			Ess ess = this.esss.value();
			meterL1.add(meter.activePowerL1.value());
			meterL2.add(meter.activePowerL2.value());
			meterL3.add(meter.activePowerL3.value());
			this.essL1.add(ess.activePowerL1.value());
			this.essL2.add(ess.activePowerL2.value());
			this.essL3.add(ess.activePowerL3.value());
			calculatedPowers[0] = meterL1.avg();
			calculatedPowers[1] = meterL2.avg();
			calculatedPowers[2] = meterL3.avg();
			calculatedPowers[0] += essL1.avg();
			calculatedPowers[1] += essL2.avg();
			calculatedPowers[2] += essL3.avg();
			if (calculatedPowers[0] >= maxReactivePowerL1.value()) {
				calculatedPowers[0] -= maxReactivePowerL1.value();
			} else if (calculatedPowers[0] <= minReactivePowerL1.value()) {
				calculatedPowers[0] -= minReactivePowerL1.value();
			} else {
				calculatedPowers[0] = 0;
			}
			if (calculatedPowers[1] >= maxReactivePowerL2.value()) {
				calculatedPowers[1] -= maxReactivePowerL2.value();
			} else if (calculatedPowers[1] <= minReactivePowerL2.value()) {
				calculatedPowers[1] -= minReactivePowerL2.value();
			} else {
				calculatedPowers[1] = 0;
			}
			if (calculatedPowers[2] >= maxReactivePowerL3.value()) {
				calculatedPowers[2] -= maxReactivePowerL3.value();
			} else if (calculatedPowers[2] <= minReactivePowerL3.value()) {
				calculatedPowers[2] -= minReactivePowerL3.value();
			} else {
				calculatedPowers[2] = 0;
			}
			// Calculate required sum values
			ess.power.setReactivePower(calculatedPowers[0], calculatedPowers[1], calculatedPowers[2]);
			ess.power.writePower(ReductionType.PERPHASE);

		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		} catch (WriteChannelException e) {
			log.warn("write failed.", e);
		}
	}

}
