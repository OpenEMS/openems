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
package io.openems.impl.controller.symmetric.powerramp;

import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;
import io.openems.core.utilities.Power;

public class PowerRampController extends Controller {

	@ConfigInfo(title = "All storage, which should be controlled", type = Ess.class)
	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this);

	@ConfigInfo(title = "The limit where the powerRamp stops.(pos/neg)", type = Integer.class)
	public ConfigChannel<Integer> pMax = new ConfigChannel<Integer>("pMax", this);
	@ConfigInfo(title = "How high the step to increase the power is.", type = Integer.class)
	public ConfigChannel<Integer> pStep = new ConfigChannel<Integer>("pStep", this);
	@ConfigInfo(title = "The cosPhi to hold.", type = Double.class)
	public ConfigChannel<Double> cosPhi = new ConfigChannel<Double>("cosPhi", this);
	@ConfigInfo(title = "How long to slee till next power step.", type = Integer.class)
	public ConfigChannel<Integer> sleep = new ConfigChannel<>("sleep", this);
	private long lastPower;
	private long lastSet;

	public PowerRampController() {
		super();
	}

	public PowerRampController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			for (Ess ess : esss.value()) {
				try {
					if (ess.gridMode.labelOptional().isPresent()
							&& ess.gridMode.labelOptional().get().equals(EssNature.OFF_GRID)) {
						lastPower = 0;
					}
					Power power = ess.power;
					if (lastSet + sleep.value() < System.currentTimeMillis()) {
						if (Math.abs(lastPower + pStep.value()) <= Math.abs(pMax.value())) {
							power.setActivePower(lastPower + pStep.value());
						} else {
							power.setActivePower(pMax.value());
						}
						lastSet = System.currentTimeMillis();
					} else {
						power.setActivePower(lastPower);
					}
					power.setReactivePower(
							ControllerUtils.calculateReactivePower(power.getActivePower(), cosPhi.value()));
					power.writePower();
					lastPower = power.getActivePower();
					log.info("Set ActivePower [" + power.getActivePower() + "] Set ReactivePower ["
							+ power.getReactivePower() + "]");
				} catch (InvalidValueException e) {
					log.error("Failed to write fixed P/Q value for Ess " + ess.id, e);
				}
			}
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		}
	}

}
