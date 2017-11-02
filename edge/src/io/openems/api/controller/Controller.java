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
package io.openems.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.DebugChannel;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.thing.Thing;

public abstract class Controller implements Thing {
	public final static String THINGID_PREFIX = "_controller";
	private static int instanceCounter = 0;
	protected final Logger log;
	private String name;
	private DebugChannel<Long> requiredTime = new DebugChannel<>("RequiredTime", this);

	/*
	 * Config
	 */
	/**
	 * Holds the priority of this controller. High value is high priority, low value is low priority.
	 *
	 */
	@ChannelInfo(title = "Priority of this controller", type = Integer.class)
	public final ConfigChannel<Integer> priority = new ConfigChannel<Integer>("priority", this);

	// TODO add "active" configchannel to be able to deactivate a controller without deleting it

	public Controller() {
		this(null);
	}

	public Controller(String thingId) {
		if (thingId == null) {
			thingId = THINGID_PREFIX + instanceCounter++;
		}
		log = LoggerFactory.getLogger(this.getClass());
		name = thingId;
	}

	protected abstract void run();

	public void executeRun() {
		long beforeRun = System.currentTimeMillis();
		try {
			run();
		} catch (Throwable e) {
			log.error("execution of Controller ["+id()+"] failed. "+e.getMessage());
		}
		requiredTime.setValue(System.currentTimeMillis() - beforeRun);
	}

	@Override
	public String id() {
		return name;
	}
}
