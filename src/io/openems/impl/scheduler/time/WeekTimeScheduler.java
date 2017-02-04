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

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.scheduler.Scheduler;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.JsonUtils;

@ThingInfo("Simple app-planner for reccuring weekly plans")
public class WeekTimeScheduler extends Scheduler {

	private ThingRepository thingRepository;

	/*
	 * JsonArray format:
	 * [{
	 * time: "08:00",
	 * controllers: [ "controller0", "controller1"]
	 * }]
	 */
	@ConfigInfo(title = "Configures the Controllers for Monday", type = JsonArray.class)
	public ConfigChannel<JsonArray> monday = new ConfigChannel<>("monday", this);
	@ConfigInfo(title = "Configures the Controllers for Tuesday", type = JsonArray.class)
	public ConfigChannel<JsonArray> tuesday = new ConfigChannel<>("tuesday", this);
	@ConfigInfo(title = "Configures the Controllers for Wednesday", type = JsonArray.class)
	public ConfigChannel<JsonArray> wednesday = new ConfigChannel<>("wednesday", this);
	@ConfigInfo(title = "Configures the Controllers for Thursday", type = JsonArray.class)
	public ConfigChannel<JsonArray> thursday = new ConfigChannel<>("thursday", this);
	@ConfigInfo(title = "Configures the Controllers for Friday", type = JsonArray.class)
	public ConfigChannel<JsonArray> friday = new ConfigChannel<>("friday", this);
	@ConfigInfo(title = "Configures the Controllers for Saturday", type = JsonArray.class)
	public ConfigChannel<JsonArray> saturday = new ConfigChannel<>("saturday", this);
	@ConfigInfo(title = "Configures the Controllers for Sunday", type = JsonArray.class)
	public ConfigChannel<JsonArray> sunday = new ConfigChannel<>("sunday", this);
	@ConfigInfo(title = "Sets the always enabled Controllers", type = JsonArray.class)
	public ConfigChannel<JsonArray> always = new ConfigChannel<>("always", this);

	public WeekTimeScheduler() {
		thingRepository = ThingRepository.getInstance();
	}

	private ConfigChannel<Integer> cycleTime = new ConfigChannel<Integer>("cycleTime", this, Integer.class)
			.defaultValue(500);

	@Override
	@ConfigInfo(title = "Sets the duration of each cycle in milliseconds", type = Integer.class)
	public ConfigChannel<Integer> cycleTime() {
		return cycleTime;
	}

	@Override
	protected void dispose() {}

	@Override
	protected void forever() {
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
					controller.run();
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
