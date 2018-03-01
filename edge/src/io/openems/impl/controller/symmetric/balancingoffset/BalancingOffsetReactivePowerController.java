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
package io.openems.impl.controller.symmetric.balancingoffset;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.power.symmetric.PowerException;

/*
 * this Controller calculates the power consumption of the house and charges or discharges the storages to reach zero power consumption from the grid
 */
@ThingInfo(title = "Balancing offset (Symmetric)", description = "Tries to keep the grid meter within an offset. For symmetric Ess.")
public class BalancingOffsetReactivePowerController extends Controller {

	private ThingStateChannels thingState = new ThingStateChannels(this);
	/*
	 * Constructors
	 */
	public BalancingOffsetReactivePowerController() {
		super();
	}

	public BalancingOffsetReactivePowerController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<>("ess", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<>("meter", this);

	@ChannelInfo(title = "Offset ReactivePower", description = "The offset of the reactive power from zero to hold on the grid meter.", type = Integer.class)
	public final ConfigChannel<Integer> reactivePowerOffset = new ConfigChannel<>("reactivePowerOffset", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			// Calculate required sum values
			long calculatedReactivePower = meter.value().reactivePower.value() + ess.reactivePower.value()
			- reactivePowerOffset.value();
			ess.reactivePowerLimit.setQ(calculatedReactivePower);
			ess.power.applyLimitation(ess.reactivePowerLimit);
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		} catch (PowerException e) {
			log.error("Failed to set Power!",e);
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return thingState;
	}

}
