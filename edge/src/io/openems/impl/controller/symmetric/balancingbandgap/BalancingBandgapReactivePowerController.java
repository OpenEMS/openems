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

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.power.symmetric.PowerException;

@ThingInfo(title = "Balancing bandgap (Symmetric)", description = "Tries to keep the grid meter within a bandgap. For symmetric Ess.")
public class BalancingBandgapReactivePowerController extends Controller {

	private final Logger log = LoggerFactory.getLogger(BalancingBandgapReactivePowerController.class);

	private ThingStateChannels thingState = new ThingStateChannels(this);
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
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ChannelInfo(title = "Min-ReactivePower", description = "Low boundary of reactive power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> minReactivePower = new ConfigChannel<>("minReactivePower", this);

	@ChannelInfo(title = "Max-ReactivePower", description = "High boundary of reactive power bandgap.", type = Integer.class)
	public final ConfigChannel<Integer> maxReactivePower = new ConfigChannel<>("maxReactivePower", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			Meter meter = this.meter.value();
			// Calculate required sum values
			long calculatedReactivePower = meter.reactivePower.value() + ess.reactivePower.value();
			if (calculatedReactivePower >= maxReactivePower.value()) {
				calculatedReactivePower -= maxReactivePower.value();
			} else if (calculatedReactivePower <= minReactivePower.value()) {
				calculatedReactivePower -= minReactivePower.value();
			} else {
				calculatedReactivePower = 0;
			}
			ess.reactivePowerLimit.setQ(calculatedReactivePower);
			ess.power.applyLimitation(ess.reactivePowerLimit);
			;
		} catch (InvalidValueException | NoSuchElementException e) {
			log.error(e.getMessage());
		} catch (PowerException e) {
			log.error("failed to set ReactivePower!", e);
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

}
