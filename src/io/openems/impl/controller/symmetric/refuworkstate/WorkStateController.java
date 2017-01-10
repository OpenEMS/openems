/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
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
package io.openems.impl.controller.symmetric.refuworkstate;

import java.util.Optional;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.api.thing.ThingDescription;

/**
 *
 * @author matthias.rossmann
 *         This Controller send the Ess a command to go in Standby if no power is Reqired.
 *         Do not use if Off-Grid Functionality is required.
 */
public class WorkStateController extends Controller {

	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);
	public final ConfigChannel<Boolean> start = new ConfigChannel<>("start", this, Boolean.class);

	private boolean reset = false;
	private long lastReset = 0L;
	private int resetCount = 0;

	public WorkStateController() {
		super();
	}

	public WorkStateController(String thingId) {
		super(thingId);
	}

	public static ThingDescription getDescription() {
		return new ThingDescription("WorkStateController",
				"Handles if the storage system should go to standby or stay in running mode. This is indicated by the configchannel 'start'. Has an error occoured tries this controller to reset the error three times. If the tries to reset the error failed the controller sleep for 30 minutes till it tries another three times to reset the error. This is repeated thill the error disapears.");
	}

	@Override public void run() {
		Ess ess;
		try {
			ess = this.ess.value();
			if (start.value()) {
				if (ess.systemState.labelOptional().equals(Optional.of(EssNature.STANDBY))) {
					ess.setWorkState.pushWriteFromLabel(EssNature.START);
					log.info("start Refu");
				} else if (!ess.systemState.labelOptional().equals(Optional.of(EssNature.START))) {
					ess.setWorkState.pushWriteFromLabel(EssNature.STOP);
				}
				if (ess.systemState.labelOptional().equals(Optional.of("Error"))
						&& lastReset <= System.currentTimeMillis() - 5000 && resetCount < 3) {
					if (!reset) {
						ess.setSystemErrorReset.pushWriteFromLabel(EssNature.ON);
						reset = true;
						log.info("Reset Refu error");
						lastReset = System.currentTimeMillis();
						resetCount++;
					}
				}
				if (reset) {
					ess.setSystemErrorReset.pushWriteFromLabel(EssNature.OFF);
					reset = false;
				}
				if (lastReset <= System.currentTimeMillis() - 30 * 60 * 1000) {
					resetCount = 0;
				}
			} else {
				ess.setWorkState.pushWriteFromLabel(EssNature.STOP);
				log.info("stop Refu");
			}
		} catch (InvalidValueException | WriteChannelException e) {
			e.printStackTrace();
		}
	}

}
