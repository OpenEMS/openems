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
package io.openems.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.thing.IsConfig;
import io.openems.api.thing.Thing;

public abstract class Controller implements Thing, Runnable {
	public final static String THINGID_PREFIX = "_controller";
	private static int instanceCounter = 0;
	protected final Logger log;
	private String name;
	private int priority = Integer.MIN_VALUE;

	public Controller() {
		log = LoggerFactory.getLogger(this.getClass());
		name = THINGID_PREFIX + instanceCounter++;
	}

	/**
	 * Returns the priority of this controller. High return value is high priority,
	 * low value is low priority.
	 *
	 * @return
	 */
	public int getPriority() {
		return this.priority;
	};

	@Override
	public String getThingId() {
		return name;
	}

	@IsConfig("priority")
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
}
