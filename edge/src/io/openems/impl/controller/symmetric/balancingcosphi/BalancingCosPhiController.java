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
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;

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
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ChannelInfo(title = "Cos-Phi", description = "Cos-phi which the grid-meter is trying to hold.", type = Double.class)
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
			Meter meter = this.meter.value();
			long currentActivePowerEss = ess.activePower.value();//50
			long currentReactivePowerEss = ess.activePower.value();//10
			long currentActivePowerGrid = meter.activePower.value();//-10
			long currentReactivePowerGrid = meter.reactivePower.value();//5
			long expectedActivePowerGrid = currentActivePowerGrid-(ess.setActivePower.getWriteValue().orElse(currentActivePowerEss)-currentActivePowerEss);//-10-(-2-50)=-62
			long expectedReactivePowerGrid = ControllerUtils.calculateReactivePower(expectedActivePowerGrid, cosPhi.value(),capacitive.value());//30,027
			long q = currentReactivePowerEss - (expectedReactivePowerGrid - currentReactivePowerGrid);//10-(30,027-5)=-15,02
			ess.power.setReactivePower(q);
			ess.power.writePower();
			log.info(ess.id() + " Set ReactivePower [" + ess.power.getReactivePower() + "]");
		} catch (InvalidValueException e) {
			log.error("Failed to read value.", e);
		}
	}
}
