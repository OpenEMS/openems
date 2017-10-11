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
package io.openems.impl.controller.symmetric.fixvalue;

import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;

@ThingInfo(title = "Fixed active and reactive power (Symmetric)", description = "Charges or discharges the battery with a predefined, fixed power. For symmetric Ess.")
public class FixValueController extends Controller {

	/*
	 * Constructors
	 */
	public FixValueController() {
		super();
	}

	public FixValueController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this);

	@ChannelInfo(title = "ActivePower", description = "The active power to set for each Ess.", type = Integer.class, isOptional = true)
	public ConfigChannel<Integer> p = new ConfigChannel<Integer>("p", this);

	@ChannelInfo(title = "ReactivePower", description = "The reactive power to set for each Ess.", type = Integer.class, isOptional = true)
	public ConfigChannel<Integer> q = new ConfigChannel<Integer>("q", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			for (Ess ess : esss.value()) {
				if (p.valueOptional().isPresent()) {
					ess.power.setActivePower(p.value());
				}
				if (q.valueOptional().isPresent()) {
					ess.power.setReactivePower(q.value());
				}
				ess.power.writePower();
			}
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		}
	}

}
