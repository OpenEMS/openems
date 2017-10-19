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
package io.openems.impl.scheduler.channelthreshold;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import info.faljse.SDNotify.SDNotify;
import io.openems.api.bridge.Bridge;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.scheduler.Scheduler;
import io.openems.core.ThingRepository;

@ThingInfo(title = "Channel threshold app-planer", description = "app-planer with thresholds on configured channel to run different controllers by threshold on channel.")
public class ChannelThresholdScheduler extends Scheduler {

	public ChannelThresholdScheduler() {
		thingRepository = ThingRepository.getInstance();
	}

	/*
	 * Fields
	 */
	private ThingRepository thingRepository;
	private ReadChannel<Long> thresholdChannel;
	private ControllerHysteresis activeHysteresis;

	/*
	 * Config
	 */

	@ChannelInfo(title = "Always", description = "Sets the controllers that are always activated.", type = JsonArray.class)
	public ConfigChannel<JsonArray> always = new ConfigChannel<>("always", this);

	/*
	 * "thresholds":[
	 * {
	 * "threshold":20,
	 * "hysteresis": 5,
	 * "controller":["ctr1","ctr3"]
	 * },
	 * {
	 * "threshold":40,
	 * "hysteresis": 7,
	 * "controller":["ctr2","ctr4"]
	 * },
	 * {
	 * "threshold":60,
	 * "hysteresis": 3,
	 * "controller":["ctr5","ctr6"]
	 * },
	 * ]
	 */

	@ChannelInfo(title = "Configures the Controllers ", type = JsonArray.class)
	public ConfigChannel<JsonArray> thresholds = new ConfigChannel<JsonArray>("thresholds", this)
	.addChangeListener((channel, newValue, oldValue) -> {
		try {
			if (isInitialized()) {
				loadThresholds();
			}
		} catch (InvalidValueException e) {
			log.error("Failed to load thresholds", e);
		}
	});

	@SuppressWarnings("unchecked")
	@ChannelInfo(title = "the address of the channel to switch the controllers by thresholds.", type = String.class)
	public ConfigChannel<String> thresholdChannelAddress = new ConfigChannel<String>("thresholdChannelAddress", this)
	.addChangeListener((channel, newValue, oldValue) -> {
		Optional<String> channelAddress = (Optional<String>) newValue;
		if (channelAddress.isPresent()) {
			Optional<Channel> ch = thingRepository.getChannelByAddress(channelAddress.get());
			if (ch.isPresent()) {
				thresholdChannel = (ReadChannel<Long>) ch.get();
			} else {
				log.error("Channel " + channelAddress.get() + " not found");
			}
		} else {
			log.error("'thresholdChannelAddress' is not configured!");
		}
	});

	/*
	 * Methods
	 */

	@Override
	protected void dispose() {}

	@Override
	protected void execute() {
		// kick the watchdog
		SDNotify.sendWatchdog();

		List<Controller> controllers = getActiveControllers();
		controllers.addAll(getAlwaysController());
		Collections.sort(controllers, (c1, c2) -> c2.priority.valueOptional().orElse(Integer.MIN_VALUE)
				- c1.priority.valueOptional().orElse(Integer.MIN_VALUE));
		for (Controller controller : controllers) {
			controller.executeRun();
		}
		for (WriteChannel<?> channel : thingRepository.getWriteChannels()) {
			channel.shadowCopyAndReset();
		}
		for (Bridge bridge : thingRepository.getBridges()) {
			bridge.triggerWrite();
		}
	}

	private List<Controller> getActiveControllers() {
		List<Controller> controllers = new ArrayList<>();
		try {
			if (activeHysteresis != null) {
				if (!activeHysteresis.isBetween(thresholdChannel.value())) {
					if (activeHysteresis.min > thresholdChannel.value()) {
						// below
						if (activeHysteresis.below != null) {
							activeHysteresis = activeHysteresis.below;
						}
					} else {
						// above
						if (activeHysteresis.above != null) {
							activeHysteresis = activeHysteresis.above;
						}
					}
				}
				controllers.addAll(activeHysteresis.controllers);
			}
		} catch (InvalidValueException e) {
			log.error("Can't read thresholdChannel.", e);
		}
		return controllers;
	}

	@Override
	protected boolean initialize() {
		try {
			loadThresholds();
		} catch (InvalidValueException e) {
			log.error("Failed to load thresholds", e);
		}
		return true;
	}

	private Controller getController(String id) {
		for (Controller c : getControllers()) {
			if (c.id().equals(id)) {
				return c;
			}
		}
		return null;
	}

	@Override
	public synchronized void addController(Controller controller) throws ReflectionException, ConfigException {
		super.addController(controller);
		if (isInitialized()) {
			try {
				loadThresholds();
			} catch (InvalidValueException e) {
				log.error("Failed to load thresholds", e);
			}
		}
	}

	@Override
	public synchronized void removeController(Controller controller) {
		super.removeController(controller);
		if (isInitialized()) {
			try {
				loadThresholds();
			} catch (InvalidValueException e) {
				log.error("Failed to load thresholds", e);
			}
		}
	}

	private void loadThresholds() throws InvalidValueException {
		List<Threshold> thresholdCollection = new ArrayList<>();
		JsonArray thresholds = this.thresholds.value();
		for (JsonElement e : thresholds) {
			if (e.isJsonObject()) {
				JsonObject thresholdJson = e.getAsJsonObject();
				Threshold t = new Threshold();
				t.threshold = thresholdJson.get("threshold").getAsLong();
				t.hysteresis = thresholdJson.get("hysteresis").getAsLong();
				JsonArray controllers = thresholdJson.get("controller").getAsJsonArray();
				for (JsonElement ctr : controllers) {
					Controller c = getController(ctr.getAsString());
					if (c != null) {
						t.controllers.add(c);
					} else {
						log.error("can't find Controller '" + ctr.getAsString() + "'!");
					}
				}
				if (t.threshold != null) {
					if (t.hysteresis != null) {
						thresholdCollection.add(t);
					} else {
						log.error("no hysteresis defined for threshold [" + t.threshold + "]!");
					}
				} else {
					log.error("threshold of element [" + e + "] is not defined.");
				}
			} else {
				log.error(e + " is no jsonobject!");
			}
		}
		Collections.sort(thresholdCollection, (c1, c2) -> c1.threshold.compareTo(c2.threshold));
		ControllerHysteresis lastHysteresis = null;
		for (Threshold t : thresholdCollection) {
			ControllerHysteresis ch = new ControllerHysteresis();
			ch.min = t.threshold;
			if (lastHysteresis != null) {
				lastHysteresis.max = t.threshold + t.hysteresis;
			}
			ch.below = lastHysteresis;
			ch.controllers.addAll(t.controllers);
			if (lastHysteresis != null) {
				lastHysteresis.above = ch;
			}
			lastHysteresis = ch;
		}
		if (lastHysteresis != null) {
			lastHysteresis.max = Long.MAX_VALUE;
		}
		if (thresholdChannel.valueOptional().isPresent() && lastHysteresis != null) {
			while (lastHysteresis.below != null) {
				if (lastHysteresis.isBetween(thresholdChannel.value())) {
					break;
				}
				lastHysteresis = lastHysteresis.below;
			}
		}
		activeHysteresis = lastHysteresis;
	}

	private List<Controller> getAlwaysController() {
		List<Controller> controller = new ArrayList<>();
		if (always.valueOptional().isPresent()) {
			for (JsonElement element : always.valueOptional().get()) {
				controller.add(controllers.get(element.getAsString()));
			}
		}
		return controller;
	}

	private class Threshold {
		public Long threshold;
		public Long hysteresis;
		public List<Controller> controllers = new ArrayList<>();
	}

	private class ControllerHysteresis {
		public Long min;
		public Long max;
		public ControllerHysteresis above;
		public ControllerHysteresis below;
		public final List<Controller> controllers = new ArrayList<>();

		public boolean isBetween(long value) {
			return min <= value && value <= max;
		}
	}
}
