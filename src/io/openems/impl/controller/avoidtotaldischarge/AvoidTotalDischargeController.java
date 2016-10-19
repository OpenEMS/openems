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
package io.openems.impl.controller.avoidtotaldischarge;

import java.util.List;

import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMapping;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class AvoidTotalDischargeController extends Controller {
	@IsThingMapping
	public List<EssMap> esss = null;

	@Override
	public void run() {
		for (EssMap ess : esss) {
			try {
				/*
				 * Calculate SetActivePower according to MinSoc
				 */
				if (ess.soc.getValue() < ess.minSoc.getValue() && ess.soc.getValue() >= ess.minSoc.getValue() - 5) {
					// SOC < minSoc && SOC > minSoc - 5
					ess.setActivePower.pushMaxWriteValue(0);
				} else if (ess.soc.getValue() < ess.minSoc.getValue() - 5) {
					// SOC < minSoc - 5
					Long currentMaxValue = ess.setActivePower.peekMaxWriteValue();
					if (currentMaxValue != null) {
						ess.setActivePower.pushMaxWriteValue(currentMaxValue / 5);
					} else {
						ess.setActivePower.pushMaxWriteValue(1000);
					}
				}
				/*
				 * Start ESS if it was stopped and we have a setActivePower command
				 */
				if (ess.setActivePower.hasWriteValue()) {
					String systemState = ess.systemState.getValueLabelOrNull();
					if (systemState == null || systemState != "Start") {
						log.info("ESS [" + ess.getThingId() + "] was stopped. Starting...");
						ess.setWorkState.pushWriteValue("Start");
					}
				}
			} catch (InvalidValueException | WriteChannelException e) {
				log.error(e.getMessage());
			}
		}
	}
}
