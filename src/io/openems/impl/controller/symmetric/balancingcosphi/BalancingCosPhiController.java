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
package io.openems.impl.controller.symmetric.balancingcosphi;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;

@ThingInfo(title = "Balancing Cos-Phi (Symmetric)", description = "Tries to keep the grid meter at a given cos-phi. For symmetric Ess.")
public class BalancingCosPhiController extends Controller {

	/*
	 * Constructors
	 */
	public BalancingCosPhiController() {
		super();
	}

	public BalancingCosPhiController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ConfigInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ConfigInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ConfigInfo(title = "Cos-Phi", description = "Cos-phi which the grid-meter is trying to hold.", type = Double.class)
	public ConfigChannel<Double> cosPhi = new ConfigChannel<Double>("cosPhi", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			double cosPhi = this.cosPhi.value();
			double phi = Math.acos(cosPhi);
			long q = (long) ((meter.value().activePower.value() * Math.tan(phi)) - meter.value().reactivePower.value())
					* -1;
			q += ess.value().reactivePower.value();
			ess.value().power.setReactivePower(q);
			ess.value().power.writePower();
			log.info(ess.id() + " Set ReactivePower [" + ess.value().power.getReactivePower() + "]");
		} catch (InvalidValueException e) {
			log.error("Failed to read value.", e);
		}
	}
}
