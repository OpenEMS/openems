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
package io.openems.impl.controller.asymmetric.avoidtotaldischarge;

import java.util.Optional;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

@ThingInfo(title = "Avoid total discharge of battery (Asymmetric)", description = "Makes sure the battery is not going into critically low state of charge. For asymmetric Ess.")
public class AvoidTotalDischargeController extends Controller {

	@ConfigInfo(title = "all ess where load reservation should work.", type = Ess.class)
	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this);

	public AvoidTotalDischargeController() {
		super();
	}

	public AvoidTotalDischargeController(String id) {
		super(id);
	}

	@Override
	public void run() {
		try {
			for (Ess ess : esss.value()) {
				/*
				 * Calculate SetActivePower according to MinSoc
				 */
				if (ess.soc.value() < ess.minSoc.value() && ess.soc.value() >= ess.minSoc.value() - 5) {
					// SOC < minSoc && SOC >= minSoc - 5
					log.info("Avoid discharge. Set ActivePower=Max[0]");
					ess.setActivePowerL1.pushWriteMax(0L);
					ess.setActivePowerL2.pushWriteMax(0L);
					ess.setActivePowerL3.pushWriteMax(0L);
				} else if (ess.soc.value() < ess.minSoc.value() - 5) {
					// SOC < minSoc - 5
					Optional<Long> currentMinValueL1 = ess.setActivePowerL1.writeMin();
					Optional<Long> currentMinValueL2 = ess.setActivePowerL2.writeMin();
					Optional<Long> currentMinValueL3 = ess.setActivePowerL3.writeMin();
					if (currentMinValueL1.isPresent()) {
						// Force Charge with minimum of MaxChargePower/5
						log.info("Force charge. Set ActivePower=Max[" + currentMinValueL1.get() / 5 + "]");
						ess.setActivePowerL1.pushWriteMax(currentMinValueL1.get() / 5);
					} else {
						log.info("Avoid discharge. Set ActivePower=Min[1000 W]");
						ess.setActivePowerL1.pushWriteMax(-1000L);
					}
					if (currentMinValueL2.isPresent()) {
						// Force Charge with minimum of MaxChargePower/5
						log.info("Force charge. Set ActivePower=Max[" + currentMinValueL2.get() / 5 + "]");
						ess.setActivePowerL2.pushWriteMax(currentMinValueL2.get() / 5);
					} else {
						log.info("Avoid discharge. Set ActivePower=Min[1000 W]");
						ess.setActivePowerL2.pushWriteMax(-1000L);
					}
					if (currentMinValueL3.isPresent()) {
						// Force Charge with minimum of MaxChargePower/5
						log.info("Force charge. Set ActivePower=Max[" + currentMinValueL3.get() / 5 + "]");
						ess.setActivePowerL3.pushWriteMax(currentMinValueL3.get() / 5);
					} else {
						log.info("Avoid discharge. Set ActivePower=Min[1000 W]");
						ess.setActivePowerL3.pushWriteMax(-1000L);
					}
				}
			}
		} catch (InvalidValueException | WriteChannelException e) {
			log.error(e.getMessage());
		}
	}

}
