package io.openems.edge.energy.task;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

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
public class ManualTask extends AbstractEnergyTask {

	private final Logger log = LoggerFactory.getLogger(Task.class);
	private final Map<String, String[]> componentsToPresetStrings;

	public ManualTask(ComponentManager componentManager, String manualSchedule, Consumer<Boolean> scheduleError) {
		super(componentManager, scheduleError);
		Map<String, String[]> componentsToPresetStrings;
		try {
			componentsToPresetStrings = parseSchedules(manualSchedule);
			scheduleError.accept(false);

		} catch (OpenemsNamedException e) {
			componentsToPresetStrings = ImmutableMap.of();
			this.log.error("Unable to parse Schedule: " + e.getMessage());
			e.printStackTrace();
			scheduleError.accept(true);
		}
		this.componentsToPresetStrings = componentsToPresetStrings;
	}

	protected static Map<String, String[]> parseSchedules(String manualSchedule) throws OpenemsNamedException {
		var result = ImmutableMap.<String, String[]>builder();
		var schedules = JsonUtils.parseToJsonObject(manualSchedule);
		for (var componentId : schedules.keySet()) {
			var presets = new ArrayList<String>();
			for (var preset : JsonUtils.getAsJsonArray(schedules.get(componentId), "presets")) {
				presets.add(JsonUtils.getAsString(preset));
			}
			result.put(componentId, presets.toArray(String[]::new));
		}
		return result.build();
	}

	@Override
	public void run() {
		var scheduleError = false;
		this.schedules = null;
		var schedules = Schedules.create();

		var now = ZonedDateTime.now(this.componentManager.getClock());
		for (var entry : this.componentsToPresetStrings.entrySet()) {
			// Get Component
			String componentId = entry.getKey();
			final OpenemsComponent component;
			try {
				component = componentManager.getComponent(componentId);
			} catch (OpenemsNamedException e) {
				this.log.warn("Unable to get Component [" + componentId + "]: " + e.getMessage());
				scheduleError = true;
				continue;
			}

			// Cast to Schedulable
			if (component instanceof Schedulable schedulable) {
				// Read Component Presets
				var scheduleHandler = schedulable.getScheduleHandler();
				var componentPresets = new HashMap<String, Preset>();
				for (var preset : scheduleHandler.presets) {
					componentPresets.put(preset.name(), preset);
				}
				// Map Presets in Schedule
				var presets = Stream.of(entry.getValue()) //
						.map(preset -> componentPresets.get(preset)) //
						.toArray(Preset[]::new);

				// Apply Schedule
				var schedule = Schedule.of(now, presets);
				schedules.add(componentId, schedule); // for debugLog
				scheduleHandler.applySchedule((Schedule<?, ?>) schedule);

			} else {
				this.log.warn("Component [" + componentId + "] is not Schedulable");
				scheduleError = true;
				continue; // Not Schedulable
			}
		}
		this.schedules = schedules.build();
		this.scheduleError.accept(scheduleError);
	}

}
