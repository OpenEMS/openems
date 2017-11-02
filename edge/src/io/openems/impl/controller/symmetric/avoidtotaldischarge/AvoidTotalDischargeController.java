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
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.impl.controller.symmetric.avoidtotaldischarge.Ess.State;

@ThingInfo(title = "Avoid total discharge of battery (Symmetric)", description = "Makes sure the battery is not going into critically low state of charge. For symmetric Ess.")
public class AvoidTotalDischargeController extends Controller {

	/*
	 * Constructors
	 */
	public AvoidTotalDischargeController() {
		super();
	}

	public AvoidTotalDischargeController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this);
	@ChannelInfo(title = "Max Soc", description = "If the System is full the charge is blocked untill the soc decrease below the maxSoc.", type = Long.class, defaultValue = "95")
	public final ConfigChannel<Long> maxSoc = new ConfigChannel<Long>("maxSoc", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			for (Ess ess : esss.value()) {
				try {
					/*
					 * Calculate SetActivePower according to MinSoc
					 */
					switch (ess.currentState) {
					case CHARGESOC:
						if (ess.soc.value() > ess.minSoc.value()) {
							ess.currentState = State.MINSOC;
						} else {
							try {
								Optional<Long> currentMinValue = ess.setActivePower.writeMin();
								if (currentMinValue.isPresent() && currentMinValue.get() < 0) {
									// Force Charge with minimum of MaxChargePower/5
									log.info("Ess [" + ess.id() + "] force charge. Set ActivePower=Max["
											+ currentMinValue.get() / 5 + "]");
									ess.setActivePower.pushWriteMax(currentMinValue.get() / 5);
								} else {
									log.info("Ess [" + ess.id() + "] Avoid discharge. Set ActivePower=Max[-1000 W]");
									ess.setActivePower.pushWriteMax(-1000L);
								}
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						break;
					case MINSOC:
						if (ess.soc.value() < ess.chargeSoc.value()) {
							ess.currentState = State.CHARGESOC;
						} else if (ess.soc.value() >= ess.minSoc.value() + 5) {
							ess.currentState = State.NORMAL;
						} else {
							try {
								long maxPower = 0;
								if (!ess.setActivePower.writeMax().isPresent()
										|| maxPower < ess.setActivePower.writeMax().get()) {
									ess.setActivePower.pushWriteMax(maxPower);
								}
							} catch (WriteChannelException e) {
								log.error("Ess [" + ess.id() + "] Failed to set Max allowed power.", e);
							}
						}
						break;
					case NORMAL:
						if (ess.soc.value() <= ess.minSoc.value()) {
							ess.currentState = State.MINSOC;
						} else if (ess.soc.value() >= 99 && ess.allowedCharge.value() == 0
								&& ess.systemState.labelOptional().equals(Optional.of(EssNature.START))) {
							ess.currentState = State.FULL;
						}
						break;
					case FULL:
						try {
							ess.setActivePower.pushWriteMin(0L);
						} catch (WriteChannelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (ess.soc.value() < maxSoc.value()) {
							ess.currentState = State.NORMAL;
						}
						break;
					}
				} catch (InvalidValueException e) {
					log.error(e.getMessage());
				}
			}
		} catch (InvalidValueException e) {
			log.error("no ess configured"+e.getMessage());
		}
	}
}
