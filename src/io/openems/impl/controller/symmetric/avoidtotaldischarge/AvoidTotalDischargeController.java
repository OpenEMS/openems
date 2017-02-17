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
package io.openems.impl.controller.symmetric.avoidtotaldischarge;

import java.util.Optional;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

@ThingInfo(title = "Avoid total discharge of battery (Symmetric)", description = "Makes sure the battery is not going into critically low state of charge. For symmetric Ess.")
public class AvoidTotalDischargeController extends Controller {

	@ConfigInfo(title = "Storages, where total discharge should be avoided. For excample to reserve load for the Off-Grid power supply.", type = Ess.class)
	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this);

	public AvoidTotalDischargeController() {
		super();
	}

	public AvoidTotalDischargeController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			for (Ess ess : esss.value()) {
				/*
				 * Calculate SetActivePower according to MinSoc
				 */
				ess.socMinHysteresis.apply(ess.soc.value(), (state, multiplier) -> {
					switch (state) {
					case ASC:
					case DESC:
						try {
							long maxPower = (long) (ess.allowedDischarge.value() * multiplier);
							if (!ess.setActivePower.writeMax().isPresent()
									|| maxPower < ess.setActivePower.writeMax().get()) {
								ess.setActivePower.pushWriteMax(maxPower);
							}
						} catch (InvalidValueException e) {
							log.error(ess.id() + "Value allowedDischarge is not present.", e);
						} catch (WriteChannelException e) {
							log.error(ess.id() + "Failed to set Max allowed power.", e);
						}
						break;
					case BELOW:
						if (ess.isChargeSoc) {
							try {
								Optional<Long> currentMinValue = ess.setActivePower.writeMin();
								if (currentMinValue.isPresent() && currentMinValue.get() < 0) {
									// Force Charge with minimum of MaxChargePower/5
									log.info("Force charge. Set ActivePower=Max[" + currentMinValue.get() / 5 + "]");
									ess.setActivePower.pushWriteMax(currentMinValue.get() / 5);
								} else {
									log.info("Avoid discharge. Set ActivePower=Max[-1000 W]");
									ess.setActivePower.pushWriteMax(-1000L);
								}
								if (ess.soc.value() >= ess.minSoc.value()) {
									ess.isChargeSoc = false;
								}
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else {
							try {
								if (ess.soc.value() < ess.chargeSoc.value()) {
									ess.isChargeSoc = true;
								} else {
									ess.setActivePower.pushWriteMax(0L);
								}
							} catch (Exception e) {
								log.error(e.getMessage());
							}
						}
						break;
					case ABOVE:
					default:
						break;
					}
				});
				// long maxWrite = ess.allowedDischarge.value();
				// if (ess.setActivePower.writeMax().isPresent() && ess.setActivePower.writeMax().get() < maxWrite) {
				// maxWrite = ess.setActivePower.writeMax().get();
				// }
				// if (ess.isChargeSoc) {
				// Optional<Long> currentMinValue = ess.setActivePower.writeMin();
				// if (currentMinValue.isPresent() && currentMinValue.get() < 0) {
				// // Force Charge with minimum of MaxChargePower/5
				// log.info("Force charge. Set ActivePower=Max[" + currentMinValue.get() / 5 + "]");
				// ess.setActivePower.pushWriteMax(currentMinValue.get() / 5);
				// } else {
				// log.info("Avoid discharge. Set ActivePower=Max[-1000 W]");
				// ess.setActivePower.pushWriteMax(-1000L);
				// }
				// if (ess.soc.value() >= ess.minSoc.value()) {
				// ess.isChargeSoc = false;
				// }
				// } else {
				// if ((ess.soc.value() <= ess.minSoc.value()
				// || (ess.soc.value() <= ess.minSoc.value() + 3 && ess.maxPowerPercent == 0))
				// && ess.soc.value() >= ess.chargeSoc.value()) {
				// log.info("Avoid discharge. Decrease ActivePower");
				// ess.maxPowerPercent -= powerDecreaseStep.value();
				// if (ess.maxPowerPercent < 0) {
				// ess.maxPowerPercent = 0;
				// }
				// ess.setActivePower.pushWriteMax(maxWrite / 100 * ess.maxPowerPercent);
				// } else if (ess.soc.value() > ess.minSoc.value()) {
				// if (ess.maxPowerPercent + powerDecreaseStep.value() < 100) {
				// ess.maxPowerPercent += powerDecreaseStep.value();
				// } else {
				// ess.maxPowerPercent = 100;
				// }
				// ess.setActivePower.pushWriteMax(maxWrite / 100 * ess.maxPowerPercent);
				// } else if (ess.soc.value() < ess.chargeSoc.value()) {
				// // SOC < minSoc - 5
				// ess.isChargeSoc = true;
				// }
				// }
			}
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}
	}
}
