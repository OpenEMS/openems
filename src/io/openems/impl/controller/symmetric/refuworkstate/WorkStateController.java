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
package io.openems.impl.controller.symmetric.refuworkstate;

import java.util.Optional;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

/**
 * @author matthias.rossmann
 */
@ThingInfo(title = "REFU Workstate (Symmetric)", description = "Sends the Ess to Standby if no power is required. Do not use if Off-Grid functionality is required. For symmetric Ess.")
public class WorkStateController extends Controller {

	/*
	 * Constructors
	 */
	public WorkStateController() {
		super();
	}

	public WorkStateController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ConfigInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<>("ess", this);

	@ConfigInfo(title = "Start/Stop", description = "Indicates if the Ess should be started (true) or stopped (false).", type = Boolean.class)
	public final ConfigChannel<Boolean> start = new ConfigChannel<>("start", this);

	/*
	 * Fields
	 */
	private boolean reset = false;
	private long lastReset = 0L;
	private int resetCount = 0;

	/*
	 * Methods
	 */
	@Override
	public void run() {
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
