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
package io.openems.impl.controller.asymmetric.fixvalue;

import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;

@ThingInfo(title = "Fixed active and reactive power (Asymmetric)", description = "Charges or discharges the battery with a predefined, fixed power. For asymmetric Ess.")
public class FixValueReactivePowerController extends Controller {

	private ThingStateChannel thingState = new ThingStateChannel(this);
	/*
	 * Constructors
	 */
	public FixValueReactivePowerController() {
		super();
	}

	public FixValueReactivePowerController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this);

	@ChannelInfo(title = "ReactivePower L1", description = "Fixed reactive power for phase L1.", type = Long.class)
	public final ConfigChannel<Long> reactivePowerL1 = new ConfigChannel<>("reactivePowerL1", this);

	@ChannelInfo(title = "ReactivePower L2", description = "Fixed reactive power for phase L2.", type = Long.class)
	public final ConfigChannel<Long> reactivePowerL2 = new ConfigChannel<>("reactivePowerL2", this);

	@ChannelInfo(title = "ReactivePower L3", description = "Fixed reactive power for phase L3.", type = Long.class)
	public final ConfigChannel<Long> reactivePowerL3 = new ConfigChannel<>("reactivePowerL3", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			for (Ess ess : esss.value()) {
				ess.power.setReactivePower(reactivePowerL1.value(), reactivePowerL2.value(), reactivePowerL3.value());
			}
		} catch (InvalidValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public ThingStateChannel getStateChannel() {
		return this.thingState;
	}

}
