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
package io.openems.impl.controller.systemstate.time;

import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.common.session.Role;

/**
 * @author matthias.rossmann
 */
@ThingInfo(title = "Keep always running", description = "Tries to keep the Ess always running. Use if Off-Grid functionality is required.")
public class TimeOnController extends Controller implements ChannelChangeListener {

	private ThingStateChannels thingState = new ThingStateChannels(this);

	/*
	 * Constructors
	 */
	public TimeOnController() {
		super();
	}

	public TimeOnController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this);

	@ChannelInfo(title = "Time-On", description = "", type = String.class, writeRoles = { Role.OWNER })
	public ConfigChannel<String> timeOn = new ConfigChannel<String>("timeOn", this).addChangeListener(this);
	@ChannelInfo(title = "Time-Off", description = "", type = String.class, writeRoles = { Role.OWNER })
	public ConfigChannel<String> timeOff = new ConfigChannel<String>("timeOff", this).addChangeListener(this);

	/*
	 * Fields
	 */
	private LocalTime timeStart;
	private LocalTime timeStop;

	enum State {
		START, STOP
	}

	private State currentState = State.STOP;

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			System.out.println(currentState.toString());
			switch (currentState) {
			case START:
				if (isOpen(timeStop, timeStart, LocalTime.now())) {
					this.currentState = State.STOP;
				} else {
					for (Ess ess : esss.value()) {
						System.out.println(ess.systemState.value());
						try {
							ess.setWorkState.pushWriteFromLabel(EssNature.START);
						} catch (WriteChannelException e) {
							log.error("", e);
						}
					}
				}
				break;
			case STOP:
				if (isOpen(timeStart, timeStop, LocalTime.now())) {
					this.currentState = State.START;
				} else {
					for (Ess ess : esss.value()) {
						System.out.println(ess.systemState.value());
						try {
							ess.setWorkState.pushWriteFromLabel(EssNature.STOP);
						} catch (WriteChannelException e) {
							log.error("", e);
						}
					}
				}
				break;
			default:
				log.error("no state set.");
				break;
			}
		} catch (InvalidValueException e) {
			log.error("No Storage Found!", e);
		}
	}

	public static boolean isOpen(LocalTime start, LocalTime end, LocalTime time) {
		if (start.isAfter(end)) {
			return !time.isBefore(start) || !time.isAfter(end);
		} else {
			return !time.isBefore(start) && !time.isAfter(end);
		}
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(timeOn)) {
			if (newValue.isPresent()) {
				timeStart = LocalTime.parse((String) newValue.get());
			} else {
				timeStart = null;
			}
		} else if (channel.equals(timeOff)) {
			if (newValue.isPresent()) {
				timeStop = LocalTime.parse((String) newValue.get());
			} else {
				timeStop = null;
			}
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}
}
