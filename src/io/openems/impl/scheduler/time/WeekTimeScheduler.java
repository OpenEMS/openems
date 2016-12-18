package io.openems.impl.scheduler.time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.scheduler.Scheduler;
import io.openems.core.ThingRepository;

public class WeekTimeScheduler extends Scheduler {

	private ThingRepository thingRepository;

	/*
	 * JsonObject format:
	 * {
	 * "08:00": ["controller0", "controller1"]
	 * }
	 */
	public ConfigChannel<JsonObject> monday = new ConfigChannel<>("monday", this, JsonObject.class);
	public ConfigChannel<JsonObject> tuesday = new ConfigChannel<>("tuesday", this, JsonObject.class);
	public ConfigChannel<JsonObject> wednesday = new ConfigChannel<>("wednesday", this, JsonObject.class);
	public ConfigChannel<JsonObject> thursday = new ConfigChannel<>("thursday", this, JsonObject.class);
	public ConfigChannel<JsonObject> friday = new ConfigChannel<>("friday", this, JsonObject.class);
	public ConfigChannel<JsonObject> saturday = new ConfigChannel<>("saturday", this, JsonObject.class);
	public ConfigChannel<JsonObject> sunday = new ConfigChannel<>("sunday", this, JsonObject.class);
	public ConfigChannel<JsonArray> always = new ConfigChannel<>("always", this, JsonArray.class);
	// TODO: always could be of type String[]

	public WeekTimeScheduler() {
		thingRepository = ThingRepository.getInstance();
	}

	@Override protected int getCycleTime() {
		return 500;
	}

	@Override protected void dispose() {}

	@Override protected void forever() {
		try {
			List<Controller> controllers = getActiveControllers();
			controllers.addAll(getAlwaysController());
			Collections.sort(controllers, (c1, c2) -> c2.priority.valueOptional().orElse(Integer.MIN_VALUE)
					- c1.priority.valueOptional().orElse(Integer.MIN_VALUE));
			for (Controller controller : controllers) {
				// TODO: check if WritableChannels can still be changed, before executing
				controller.run();
			}
			for (WriteChannel<?> channel : thingRepository.getWriteChannels()) {
				channel.shadowCopyAndReset();
			}
			for (Bridge bridge : thingRepository.getBridges()) {
				bridge.triggerWrite();
			}
		} catch (InvalidValueException | DateTimeParseException | ConfigException e) {
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

	private List<Controller> getActiveControllers() throws InvalidValueException, ConfigException {
		JsonObject day = getJsonOfDay(LocalDate.now().getDayOfWeek());
		LocalTime time = LocalTime.now();
		List<Controller> controllers = null;
		int count = 1;
		while (controllers == null && count < 7) {
			try {
				controllers = floorController(day, time);
			} catch (IndexOutOfBoundsException e) {
				time = LocalTime.MAX;
				day = getJsonOfDay(LocalDate.now().getDayOfWeek().minus(count));
			}
			count++;
		}
		return controllers;
	}

	private JsonObject getJsonOfDay(DayOfWeek day) throws InvalidValueException {
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

	private List<Controller> floorController(JsonObject day, LocalTime time) throws ConfigException {
		TreeMap<LocalTime, JsonElement> times = new TreeMap<>();
		for (Entry<String, JsonElement> entry : day.entrySet()) {
			times.put(LocalTime.parse(entry.getKey()), entry.getValue());
		}
		if (times.floorEntry(time) != null) {
			List<Controller> controller = new ArrayList<>();
			for (JsonElement element : times.floorEntry(time).getValue().getAsJsonArray()) {
				Controller c = controllers.get(element.getAsString());
				if (c != null) {
					controller.add(c);
				} else {
					throw new ConfigException("Controller " + element.getAsString() + " not found");
				}
			}
			return controller;
		} else {
			throw new IndexOutOfBoundsException("No smaller time found");
		}
	}

	@Override protected boolean initialize() {
		return true;
	}

}
