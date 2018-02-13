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
import io.openems.api.channel.thingstate.ThingStateChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.power.PowerException;
import io.openems.impl.controller.symmetric.avoidtotaldischarge.Ess.State;

@ThingInfo(title = "Avoid total discharge of battery (Symmetric)", description = "Makes sure the battery is not going into critically low state of charge. For symmetric Ess.")
public class AvoidTotalDischargeController extends Controller {

	private ThingStateChannel thingState = new ThingStateChannel(this);
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
								ess.maxActivePowerLimit.setP(ess.maxNominalPower.valueOptional().orElse(-1000L));
								ess.power.applyLimitation(ess.maxActivePowerLimit);
							} catch (PowerException e) {
								log.error("Failed to set Power!",e);
							}
						}
						break;
					case MINSOC:
						if (ess.soc.value() < ess.chargeSoc.value()) {
							ess.currentState = State.CHARGESOC;
						} else if (ess.soc.value() >= ess.minSoc.value() + 5) {
							ess.currentState = State.NORMAL;
						} else {
							ess.maxActivePowerLimit.setP(0L);
							try {
								ess.power.applyLimitation(ess.maxActivePowerLimit);
							} catch (PowerException e) {
								log.error("Failed to set Power!",e);
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
						ess.minActivePowerLimit.setP(0L);
						try {
							ess.power.applyLimitation(ess.minActivePowerLimit);
						} catch (PowerException e) {
							log.error("Failed to set Power!",e);
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

	@Override
	public ThingStateChannel getStateChannel() {
		return this.thingState;
	}
}
