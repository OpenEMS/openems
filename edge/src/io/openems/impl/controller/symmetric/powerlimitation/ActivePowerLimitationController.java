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
package io.openems.impl.controller.symmetric.powerlimitation;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.power.PowerException;

@ThingInfo(title = "Power limitation (Symmetric)", description = "Limits the active and reactive power of the Ess. For symmetric Ess.")
public class ActivePowerLimitationController extends Controller {

	private ThingStateChannel thingState = new ThingStateChannel(this);
	/*
	 * Constructors
	 */
	public ActivePowerLimitationController() {
		super();
	}

	public ActivePowerLimitationController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Min-Charge ActivePower", description = "The minimum allowed active power for discharge. Value is negative.", type = Long.class, isOptional = true)
	public ConfigChannel<Long> pMin = new ConfigChannel<Long>("pMin", this);

	@ChannelInfo(title = "Max-Charge ActivePower", description = "The maximum allowed active power for discharge. Value is positive.", type = Long.class, isOptional = true)
	public ConfigChannel<Long> pMax = new ConfigChannel<Long>("pMax", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			if (pMin.isValuePresent()) {
				ess.minActivePowerLimit.setP(pMin.valueOptional().orElse(null));
				try {
					ess.power.applyLimitation(ess.minActivePowerLimit);
				} catch (PowerException e) {
					log.error("Failed to write Min P",e);
				}
			}
			if(pMax.isValuePresent()) {
				ess.maxActivePowerLimit.setP(pMax.valueOptional().orElse(null));
				try {
					ess.power.applyLimitation(ess.maxActivePowerLimit);
				} catch (PowerException e) {
					log.error("Failed to write Max P",e);
				}
			}
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		}
	}

	@Override
	public ThingStateChannel getStateChannel() {
		return thingState;
	}

}
