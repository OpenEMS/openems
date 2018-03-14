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
package io.openems.impl.controller.symmetric.balancingbandgap;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.power.symmetric.PowerException;

@ThingInfo(title = "Balancing bandgap (Symmetric)", description = "Tries to keep the grid meter within a bandgap. For symmetric Ess.")
public class BalancingBandgapActivePowerController extends Controller {

	private ThingStateChannels thingState = new ThingStateChannels(this);
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
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ChannelInfo(title = "Min-ActivePower", description = "Low boundary of active power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> minActivePower = new ConfigChannel<>("minActivePower", this);

	@ChannelInfo(title = "Max-ActivePower", description = "High boundary of active power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> maxActivePower = new ConfigChannel<>("maxActivePower", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			Meter meter = this.meter.value();
			// Calculate required sum values
			long calculatedPower = meter.activePower.value() + ess.activePower.value();
			if (calculatedPower >= maxActivePower.value()) {
				calculatedPower -= maxActivePower.value();
			} else if (calculatedPower <= minActivePower.value()) {
				calculatedPower -= minActivePower.value();
			} else {
				calculatedPower = 0;
			}
			ess.activePowerLimit.setP(calculatedPower);
			ess.power.applyLimitation(ess.activePowerLimit);
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		} catch (PowerException e) {
			log.error("limit power failed!", e);
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return thingState;
	}

}
