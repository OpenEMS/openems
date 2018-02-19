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
package io.openems.impl.controller.systemstate.powerthreshold;

import java.util.Set;

import com.google.common.base.Optional;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.impl.controller.systemstate.powerthreshold.Ess.State;

/**
 * @author matthias.rossmann
 */
@ThingInfo(title = "Stop if not useable", description = "Starts the ess if the GridFeed power is lager than a defined threshold. The ess will be stoped if the ess are empty and the GridFeed power is below a defined threshold.")
public class ThresholdOnController extends Controller {

	private ThingStateChannels thingState = new ThingStateChannels(this);

	/*
	 * Constructors
	 */
	public ThresholdOnController() {
		super();
	}

	public ThresholdOnController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ChannelInfo(title = "Power Threshold Start", description = "If the Grid-Feed Power is lager than this Threshold, the System will be started", type = Long.class)
	public final ConfigChannel<Long> onThreshold = new ConfigChannel<>("onThreshold", this);
	@ChannelInfo(title = "Power Threshold Stop", description = "If the Grid-Feed Power is smaler than this Threshold and the ess is empty, the System will be stopped.", type = Long.class)
	public final ConfigChannel<Long> offThreshold = new ConfigChannel<>("offThreshold", this);
	@ChannelInfo(title = "Time Lag Start", description = "The time the power has to be above the onThreshold to start the ess.", type = Long.class)
	public final ConfigChannel<Long> onTimelag = new ConfigChannel<>("onTimelag", this);
	@ChannelInfo(title = "Time Lag Stop", description = "The time,in minutes, the power has to be below the offThreshold to stop the ess.", type = Long.class)
	public final ConfigChannel<Long> offTimelag = new ConfigChannel<>("offTimelag", this);

	/*
	 * Fields
	 */
	private long timePowerAboveStartThreshold = System.currentTimeMillis();
	private long timePowerBelowStopThreshold = System.currentTimeMillis();

	/*
	 * Methods
	 */
	@SuppressWarnings("unlikely-arg-type")
	@Override
	public void run() {
		try {
			long calculatedPower = meter.value().getPower();
			for (Ess ess : esss.value()) {
				calculatedPower += ess.getPower();
			}
			if (calculatedPower >= onThreshold.value()) {
				timePowerAboveStartThreshold = System.currentTimeMillis();
			}
			if (calculatedPower < offThreshold.value()) {
				timePowerBelowStopThreshold = System.currentTimeMillis();
			}
			for (Ess ess : esss.value()) {
				switch (ess.currentState) {
				case OFF:
					if (System.currentTimeMillis() - timePowerAboveStartThreshold > onTimelag.value() * 1000 * 60) {
						ess.currentState = State.ON;
					} else {
						try {
							ess.setWorkState.pushWriteFromLabel(EssNature.STOP);
						} catch (WriteChannelException e) {
							log.error("", e);
						}
					}
					break;
				case ON:
					if (System.currentTimeMillis() - timePowerBelowStopThreshold > offTimelag.value() * 1000 * 60
							&& ess.soc.value() <= ess.minSoc.value()) {
						ess.currentState = State.OFF;
					} else {
						try {
							ess.setWorkState.pushWriteFromLabel(EssNature.START);
						} catch (WriteChannelException e) {
							log.error("", e);
						}
					}
					break;
				case UNKNOWN:
				default:
					if (ess.systemState.labelOptional().equals(Optional.of(EssNature.STOP))) {
						ess.currentState = State.OFF;
					} else {
						ess.currentState = State.ON;
					}
					break;
				}

			}
		} catch (InvalidValueException e) {
			log.error("No Storage Found!", e);
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}
}
