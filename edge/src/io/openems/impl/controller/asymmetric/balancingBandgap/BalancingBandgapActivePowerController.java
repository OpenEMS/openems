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

@ThingInfo(title = "Self-consumption optimization (Asymmetric)", description = "Tries to keep the grid meter on zero. For asymmetric Ess.")
public class BalancingBandgapActivePowerController extends Controller {

	/*
	 * Constructors
	 */
	public BalancingBandgapActivePowerController() {
		super();
	}

	public BalancingBandgapActivePowerController(String thingId) {
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

	@ChannelInfo(title = "Max-ActivePowerL1", description = "High boundary of active power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> maxActivePowerL1 = new ConfigChannel<>("maxActivePowerL1", this);

	@ChannelInfo(title = "Min-ReactivePowerL1", description = "Low boundary of reactive power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> minActivePowerL1 = new ConfigChannel<>("minActivePowerL1", this);

	@ChannelInfo(title = "Max-ActivePowerL2", description = "High boundary of active power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> maxActivePowerL2 = new ConfigChannel<>("maxActivePowerL2", this);

	@ChannelInfo(title = "Min-ReactivePowerL2", description = "Low boundary of reactive power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> minActivePowerL2 = new ConfigChannel<>("minActivePowerL2", this);

	@ChannelInfo(title = "Max-ActivePowerL3", description = "High boundary of active power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> maxActivePowerL3 = new ConfigChannel<>("maxActivePowerL3", this);

	@ChannelInfo(title = "Min-ReactivePowerL3", description = "Low boundary of reactive power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> minActivePowerL3 = new ConfigChannel<>("minActivePowerL3", this);

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
			calculatedPowers[0] = meter.activePowerL1.value();
			calculatedPowers[1] = meter.activePowerL2.value();
			calculatedPowers[2] = meter.activePowerL3.value();
			calculatedPowers[0] += ess.activePowerL1.value();
			calculatedPowers[1] += ess.activePowerL2.value();
			calculatedPowers[2] += ess.activePowerL3.value();
			if (calculatedPowers[0] >= maxActivePowerL1.value()) {
				calculatedPowers[0] -= maxActivePowerL1.value();
			} else if (calculatedPowers[0] <= minActivePowerL1.value()) {
				calculatedPowers[0] -= minActivePowerL1.value();
			} else {
				calculatedPowers[0] = 0;
			}
			if (calculatedPowers[1] >= maxActivePowerL2.value()) {
				calculatedPowers[1] -= maxActivePowerL2.value();
			} else if (calculatedPowers[1] <= minActivePowerL2.value()) {
				calculatedPowers[1] -= minActivePowerL2.value();
			} else {
				calculatedPowers[1] = 0;
			}
			if (calculatedPowers[2] >= maxActivePowerL3.value()) {
				calculatedPowers[2] -= maxActivePowerL3.value();
			} else if (calculatedPowers[2] <= minActivePowerL3.value()) {
				calculatedPowers[2] -= minActivePowerL3.value();
			} else {
				calculatedPowers[2] = 0;
			}
			// Calculate required sum values
			ess.power.setActivePower(calculatedPowers[0], calculatedPowers[1], calculatedPowers[2]);
			ess.power.writePower();

		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}
	}

}
