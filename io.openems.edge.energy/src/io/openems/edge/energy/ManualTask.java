package io.openems.edge.energy;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.schedulable.Schedulable;
import io.openems.edge.energy.api.schedulable.Schedule;
import io.openems.edge.energy.api.schedulable.Schedule.Preset;
import io.openems.edge.energy.api.schedulable.Schedules;
import okhttp3.internal.concurrent.Task;

/**
 * This task is executed once in the beginning and afterwards every full 15
 * minutes.
 */
public class ManualTask implements Runnable {

	private final Logger log = LoggerFactory.getLogger(Task.class);
	private final ComponentManager componentManager;
	private final Map<String, String[]> componentsToPresetStrings;

	private Schedules schedules;

	public ManualTask(ComponentManager componentManager, String manualSchedule) throws OpenemsNamedException {
		this.componentManager = componentManager;
		this.componentsToPresetStrings = parseSchedules(manualSchedule);
	}

	protected static Map<String, String[]> parseSchedules(String manualSchedule) throws OpenemsNamedException {
		var result = ImmutableMap.<String, String[]>builder();
		var schedules = JsonUtils.parseToJsonObject(manualSchedule);
		for (var componentId : schedules.keySet()) {
			var presets = JsonUtils.stream(JsonUtils.getAsJsonArray(schedules, "presets")) //
					.map(JsonElement::toString) //
					.toArray(String[]::new);
			result.put(componentId, presets);
		}
		return result.build();
	}

	@Override
	public void run() {
		this.schedules = null;
		var schedules = Schedules.create();

		var thisDay = ZonedDateTime.now(this.componentManager.getClock()).truncatedTo(ChronoUnit.DAYS);
		for (var entry : this.componentsToPresetStrings.entrySet()) {
			// Get Component
			String componentId = entry.getKey();
			final OpenemsComponent component;
			try {
				component = componentManager.getComponent(componentId);
			} catch (OpenemsNamedException e) {
				this.log.warn("Unable to get Component [" + componentId + "]: " + e.getMessage());
				continue;
			}

			// Cast to Schedulable
			if (component instanceof Schedulable schedulable) {
				// Read Component Presets
				var scheduleHandler = schedulable.getScheduleHandler();
				var componentPresets = Stream.of(scheduleHandler.presets) //
						.collect(Collectors.toUnmodifiableMap(Preset::name, Function.identity(), (t, u) -> u));

				// Map Presets in Schedule
				var presets = Stream.of(entry.getValue()) //
						.map(preset -> componentPresets.get(preset)) //
						.toArray(Preset[]::new);

				// Apply Schedule
				var schedule = Schedule.of(thisDay, presets);
				schedules.add(componentId, schedule); // for debugLog
				scheduleHandler.applySchedule((Schedule<?, ?>) schedule);

			} else
				continue; // Not Schedulable
		}

		this.schedules = schedules.build();
	}

	/**
	 * Gets a debug log message.
	 * 
	 * @return a message
	 */
	public String debugLog() {
		var schedules = this.schedules;
		if (schedules != null) {
			return this.schedules.debugLog();
		} else {
			return "";
		}
	}

}
