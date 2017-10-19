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
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.AsymmetricPower.ReductionType;

@ThingInfo(title = "Fixed active and reactive power (Asymmetric)", description = "Charges or discharges the battery with a predefined, fixed power. For asymmetric Ess.")
public class FixValueActivePowerController extends Controller {

	/*
	 * Constructors
	 */
	public FixValueActivePowerController() {
		super();
	}

	public FixValueActivePowerController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this);

	@ChannelInfo(title = "ActivePower L1", description = "Fixed active power for phase L1.", type = Long.class)
	public final ConfigChannel<Long> activePowerL1 = new ConfigChannel<>("activePowerL1", this);

	@ChannelInfo(title = "ActivePower L2", description = "Fixed active power for phase L2.", type = Long.class)
	public final ConfigChannel<Long> activePowerL2 = new ConfigChannel<>("activePowerL2", this);

	@ChannelInfo(title = "ActivePower L3", description = "Fixed active power for phase L3.", type = Long.class)
	public final ConfigChannel<Long> activePowerL3 = new ConfigChannel<>("activePowerL3", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			for (Ess ess : esss.value()) {
				ess.power.setActivePower(activePowerL1.value(), activePowerL2.value(), activePowerL3.value());
				ess.power.writePower(ReductionType.PERPHASE);
			}
		} catch (InvalidValueException e) {
			log.error("Failed to read Value", e);
		} catch (WriteChannelException e) {
			log.warn("set Power failed!");
		}
	}

}
