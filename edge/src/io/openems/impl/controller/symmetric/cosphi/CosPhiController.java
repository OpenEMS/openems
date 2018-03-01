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
package io.openems.impl.controller.symmetric.cosphi;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.power.symmetric.PowerException;
import io.openems.impl.controller.symmetric.balancingcosphi.Ess;

@ThingInfo(title = "Ess Cos-Phi (Symmetric)", description = "Keeps the Ess at a given cos-phi. For symmetric Ess.")
public class CosPhiController extends Controller {

	private ThingStateChannels thingState = new ThingStateChannels(this);
	/*
	 * Constructors
	 */
	public CosPhiController() {
		super();
	}

	public CosPhiController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Cos-Phi", description = "The cos-phi to hold on the storage.", type = Double.class)
	public ConfigChannel<Double> cosPhi = new ConfigChannel<Double>("cosPhi", this);

	@ChannelInfo(title = "Capacitive CosPhi", description="if this value is true the cosPhi is capacitive otherwise inductive.",type=Boolean.class)
	public ConfigChannel<Boolean> capacitive = new ConfigChannel<Boolean>("capacitive",this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			ess.limit.setCosPhi(cosPhi.valueOptional().orElse(null), capacitive.valueOptional().orElse(null), 0L, 0L);
			ess.power.applyLimitation(ess.limit);
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		} catch (PowerException e) {
			log.error("Failed to set Power!",e);
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

}
