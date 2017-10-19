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
package io.openems.api.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.DebugChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.thing.Thing;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.AbstractWorker;

public abstract class Scheduler extends AbstractWorker implements Thing {
	private final static int DEFAULT_CYCLETIME = 1000;
	public final static String THINGID_PREFIX = "_scheduler";
	private static int instanceCounter = 0;
	protected final Map<String, Controller> controllers = new ConcurrentHashMap<>();
	private long requiredTime = 0;
	private long cycleStartTime = 0;
	protected final ThingRepository thingRepository;
	private Integer actualCycleTime = null;
	private DebugChannel<Long> requiredCycleTime = new DebugChannel<>("RequiredCycleTime", this);

	/*
	 * Config
	 */
	@ChannelInfo(title = "Sets the duration of each cycle in milliseconds", type = Integer.class, isOptional = true)
	public ConfigChannel<Integer> cycleTime = new ConfigChannel<Integer>("cycleTime", this)
	.defaultValue(DEFAULT_CYCLETIME);

	@Override
	protected int getCycleTime() {
		int time = cycleTime.valueOptional().orElse(DEFAULT_CYCLETIME);
		if (actualCycleTime != null) {
			time = actualCycleTime;
		}
		return time;
	}

	public Scheduler() {
		super(THINGID_PREFIX + instanceCounter++);
		thingRepository = ThingRepository.getInstance();
	}

	public synchronized void addController(Controller controller) throws ReflectionException, ConfigException {
		controllers.put(controller.id(), controller);
	}

	public synchronized void removeController(Controller controller) {
		controllers.remove(controller.id());
	}

	public synchronized List<Controller> getControllers() {
		return Collections.unmodifiableList(new ArrayList<>(this.controllers.values()));
	}

	@Override
	protected void forever() {
		cycleStartTime = System.currentTimeMillis();
		execute();
		requiredTime = System.currentTimeMillis() - cycleStartTime;
		long maxTime = 0;
		for (Bridge bridge : thingRepository.getBridges()) {
			if (bridge.getRequiredCycleTime() > maxTime) {
				maxTime = bridge.getRequiredCycleTime();
			}
		}
		maxTime = (maxTime + 100) / 100 * 100;
		if (maxTime > cycleTime.valueOptional().orElse(500)) {
			// prevent cycleTime to get too big otherwise stuck bridge stops whole framework
			if (maxTime > cycleTime.valueOptional().orElse(500) * 3) {
				actualCycleTime = cycleTime.valueOptional().orElse(500) * 3;
			} else {
				actualCycleTime = (int) maxTime;
			}
		} else {
			actualCycleTime = cycleTime.valueOptional().orElse(500);
		}
		requiredCycleTime.setValue(System.currentTimeMillis() - cycleStartTime);
	}

	protected abstract void execute();

	public long getRequiredTime() {
		return requiredTime;
	}

	public long getNextCycleStart() {
		return cycleStartTime + getCycleTime();
	}

	public long getCycleStartTime() {
		return cycleStartTime;
	}
}
