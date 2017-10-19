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
package io.openems.impl.scheduler.time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import info.faljse.SDNotify.SDNotify;
import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.scheduler.Scheduler;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.JsonUtils;

@ThingInfo(title = "Weekly App-Planner", description = "Define recurring weekly plans.")
public class WeekTimeScheduler extends Scheduler {

	/*
	 * Constructors
	 */
	public WeekTimeScheduler() {
		thingRepository = ThingRepository.getInstance();
	}

	/*
	 * Config
	 */
	/*
	 * JsonArray format:
	 * [{
	 * time: "08:00",
	 * controllers: [ "controller0", "controller1"]
	 * }]
	 */
	@ChannelInfo(title = "Monday", description = "Sets the controllers for monday.", type = JsonArray.class)
	public ConfigChannel<JsonArray> monday = new ConfigChannel<>("monday", this);

	@ChannelInfo(title = "Tuesday", description = "Sets the controllers for tuesday.", type = JsonArray.class)
	public ConfigChannel<JsonArray> tuesday = new ConfigChannel<>("tuesday", this);

	@ChannelInfo(title = "Wednesday", description = "Sets the controllers for wednesday.", type = JsonArray.class)
	public ConfigChannel<JsonArray> wednesday = new ConfigChannel<>("wednesday", this);

	@ChannelInfo(title = "Thursday", description = "Sets the controllers for thursday.", type = JsonArray.class)
	public ConfigChannel<JsonArray> thursday = new ConfigChannel<>("thursday", this);

	@ChannelInfo(title = "Friday", description = "Sets the controllers for friday.", type = JsonArray.class)
	public ConfigChannel<JsonArray> friday = new ConfigChannel<>("friday", this);

	@ChannelInfo(title = "Saturday", description = "Sets the controllers for saturday.", type = JsonArray.class)
	public ConfigChannel<JsonArray> saturday = new ConfigChannel<>("saturday", this);

	@ChannelInfo(title = "Sunday", description = "Sets the controllers for sunday.", type = JsonArray.class)
	public ConfigChannel<JsonArray> sunday = new ConfigChannel<>("sunday", this);

	@ChannelInfo(title = "Always", description = "Sets the controllers that are always activated.", type = JsonArray.class)
	public ConfigChannel<JsonArray> always = new ConfigChannel<>("always", this);

	/*
	 * Fields
	 */
	private ThingRepository thingRepository;

	/*
	 * Methods
	 */
	@Override
	protected void dispose() {}

	@Override
	protected void execute() {
		// kick the watchdog
		SDNotify.sendWatchdog();

		try {
			List<Controller> controllers = getActiveControllers();
			controllers.addAll(getAlwaysController());
			Collections.sort(controllers, (c1, c2) -> {
				if (c1 == null || c2 == null) {
					return 0;
				}
				return c2.priority.valueOptional().orElse(Integer.MIN_VALUE)
						- c1.priority.valueOptional().orElse(Integer.MIN_VALUE);
			});
			for (Controller controller : controllers) {
				// TODO: check if WritableChannels can still be changed, before executing
				if (controller != null) {
					controller.executeRun();
				}
			}
			for (WriteChannel<?> channel : thingRepository.getWriteChannels()) {
				channel.shadowCopyAndReset();
			}
			for (Bridge bridge : thingRepository.getBridges()) {
				bridge.triggerWrite();
			}
		} catch (InvalidValueException | DateTimeParseException | ConfigException | ReflectionException e) {
			log.error(e.getMessage());
		}
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

	private List<Controller> getActiveControllers() throws InvalidValueException, ConfigException, ReflectionException {
		JsonArray jHours = getJsonOfDay(LocalDate.now().getDayOfWeek());
		LocalTime time = LocalTime.now();
		List<Controller> controllers = new ArrayList<>();
		int count = 1;
		while (controllers.size() == 0 && count < 8) {
			try {
				controllers.addAll(floorController(jHours, time));
			} catch (IndexOutOfBoundsException e) {
				time = LocalTime.MAX;
				jHours = getJsonOfDay(LocalDate.now().getDayOfWeek().minus(count));
			}
			count++;
		}
		return controllers;
	}

	private JsonArray getJsonOfDay(DayOfWeek day) throws InvalidValueException {
		switch (day) {
		case FRIDAY:
			return friday.value();
		case SATURDAY:
			return saturday.value();
		case SUNDAY:
			return sunday.value();
		case THURSDAY:
			return thursday.value();
		case TUESDAY:
			return tuesday.value();
		case WEDNESDAY:
			return wednesday.value();
		default:
		case MONDAY:
			return monday.value();
		}
	}

	private List<Controller> floorController(JsonArray jHours, LocalTime time)
			throws ConfigException, ReflectionException {
		// fill times map; sorted by hour
		TreeMap<LocalTime, JsonArray> times = new TreeMap<>();
		for (JsonElement jHourElement : jHours) {
			JsonObject jHour = JsonUtils.getAsJsonObject(jHourElement);
			String hourTime = JsonUtils.getAsString(jHour, "time");
			JsonArray jControllers = JsonUtils.getAsJsonArray(jHourElement, "controllers");
			times.put(LocalTime.parse(hourTime), jControllers);
		}
		// return matching controllers
		if (times.floorEntry(time) != null) {
			List<Controller> controllers = new ArrayList<>();
			for (JsonElement jControllerElement : times.floorEntry(time).getValue()) {
				String controllerId = JsonUtils.getAsString(jControllerElement);
				Controller controller = this.controllers.get(controllerId);
				if (controller != null) {
					controllers.add(controller);
				} else {
					throw new ConfigException("Controller [" + controllerId + "] not found.");
				}
			}
			return controllers;
		} else {
			throw new IndexOutOfBoundsException("No smaller time found");
		}
	}

	@Override
	protected boolean initialize() {
		return true;
	}

	@Override
	public synchronized void removeController(Controller controller) {
		// remove controller from all times
		super.removeController(controller);
	}
}
