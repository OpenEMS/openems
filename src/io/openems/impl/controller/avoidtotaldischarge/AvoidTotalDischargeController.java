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

import java.util.Optional;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class AvoidTotalDischargeController extends Controller {

	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this, Ess.class);

	@Override public void run() {
		try {
			for (Ess ess : esss.value()) {
				/*
				 * Calculate SetActivePower according to MinSoc
				 */
				if (ess.soc.value() < ess.minSoc.value() && ess.soc.value() >= ess.minSoc.value() - 5) {
					// SOC < minSoc && SOC >= minSoc - 5
					log.info("Avoid discharge. Set ActivePower=Max[0]");
					ess.setActivePower.pushWriteMax(0L);
				} else if (ess.soc.value() < ess.minSoc.value() - 5) {
					// SOC < minSoc - 5
					Optional<Long> currentMinValue = ess.setActivePower.writeMin();
					if (currentMinValue.isPresent()) {
						// Force Charge with minimum of MaxChargePower/5
						log.info("Force charge. Set ActivePower=Max[" + currentMinValue.get() / 5 + "]");
						ess.setActivePower.pushWriteMax(currentMinValue.get() / 5);
					} else {
						log.info("Avoid discharge. Set ActivePower=Min[1000 W]");
						ess.setActivePower.pushWriteMax(-1000L);
					}
				}
			}
		} catch (InvalidValueException | WriteChannelException e) {
			log.error(e.getMessage());
		}
	}
}
