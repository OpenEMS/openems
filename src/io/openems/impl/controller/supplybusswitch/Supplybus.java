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
package io.openems.impl.controller.supplybusswitch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.WriteChannel;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class Supplybus {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private HashMap<Ess, WriteChannel<Boolean>> switchEssMapping;

	private Ess activeEss;

	enum State {
		CONNECTED, DISCONNECTING, DISCONNECTED, CONNECTING
	}

	private State state;

	private String name;

	public Supplybus(HashMap<Ess, WriteChannel<Boolean>> switchEssMapping, String name) {
		this.switchEssMapping = switchEssMapping;
		this.name = name;
		state = State.DISCONNECTING;
		try {
			activeEss = getActiveEss();
			if (activeEss != null) {
				state = State.CONNECTED;
				activeEss.setActiveSupplybus(this);
			}
		} catch (SupplyBusException e) {
			disconnect();
		}
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getName() {
		return name;
	}

	public Ess getActiveEss() throws SupplyBusException {
		List<Ess> activeEss = new ArrayList<>();
		for (Entry<Ess, WriteChannel<Boolean>> entry : switchEssMapping.entrySet()) {
			try {
				if (entry.getValue().value()) {
					activeEss.add(entry.getKey());
				}
			} catch (InvalidValueException e) {
				log.error("Failed to read digital output " + entry.getValue().address(), e);
			}
		}
		if (activeEss.size() > 1) {
			throw new SupplyBusException("there are more than one ess connected to the supply bus.", activeEss, this);
		} else if (activeEss.size() == 0) {
			return null;
		} else {
			return activeEss.get(0);
		}
	}

	public void disconnect() {
		for (Entry<Ess, WriteChannel<Boolean>> entry : switchEssMapping.entrySet()) {
			try {
				entry.getValue().pushWrite(false);
			} catch (WriteChannelException e) {
				log.error("Failed to write disconnect command to digital output " + entry.getValue().address(), e);
			}
		}
		if (activeEss != null) {
			activeEss.setActiveSupplybus(null);
		}
		activeEss = null;
		state = State.DISCONNECTING;
	}

	public boolean isConnected() {
		if (activeEss != null) {
			WriteChannel<Boolean> sOn = switchEssMapping.get(activeEss);
			if (sOn.valueOptional().isPresent() && sOn.valueOptional().get()) {
				state = State.CONNECTED;
				return true;
			}
		}
		return false;
	}

	public boolean isDisconnected() {
		try {
			if (getActiveEss() == null) {
				state = State.DISCONNECTED;
				return true;
			}
		} catch (SupplyBusException e) {
			return false;
		}
		return false;
	}

	public void connect(Ess ess) throws SupplyBusException {
		if (getActiveEss() == null && activeEss == null && ess != null) {
			activeEss = ess;
			activeEss.setActiveSupplybus(this);
			WriteChannel<Boolean> sOn = switchEssMapping.get(ess);
			state = State.CONNECTING;
			try {
				sOn.pushWrite(true);
			} catch (WriteChannelException e) {
				log.error("Failed to write connect command to digital output " + sOn.address(), e);
			}
		}
	}
}
