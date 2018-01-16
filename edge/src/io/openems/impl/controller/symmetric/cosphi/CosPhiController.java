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
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;
import io.openems.impl.controller.symmetric.balancingcosphi.Ess;

@ThingInfo(title = "Ess Cos-Phi (Symmetric)", description = "Keeps the Ess at a given cos-phi. For symmetric Ess.")
public class CosPhiController extends Controller {

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
			if (ess.setActivePower.peekWrite().isPresent()) {
				long expectedActivePower = ess.setActivePower.peekWrite().get();//40
				long q = ControllerUtils.calculateReactivePower(expectedActivePower, cosPhi.value(), capacitive.value());
				ess.power.setReactivePower(q);
				ess.power.writePower();
				log.info("Set ReactivePower [" + ess.power.getReactivePower() + "]");
			} else {
				log.error(ess.id() + " no ActivePower is Set.");
			}
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		}
	}

}
