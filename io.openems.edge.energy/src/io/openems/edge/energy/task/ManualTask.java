package io.openems.edge.energy.task;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
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

	private static final int NUMBER_OF_HOURS = 24;
	private static final int NUMBER_OF_QUARTERS = 96;

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
				var schedule = toSchedule(now, entry.getValue(), scheduleHandler.presets);
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

	/**
	 * Parses a String containing a JsonObject to a Map of Schedules.
	 * 
	 * @param json the {@link JsonObject}
	 * @return a Map of Component-IDs to array of String-Presets
	 * @throws OpenemsNamedException on error
	 */
	protected static Map<String, String[]> parseSchedules(String json) throws OpenemsNamedException {
		var result = ImmutableMap.<String, String[]>builder();
		var schedules = JsonUtils.parseToJsonObject(json);
		for (var componentId : schedules.keySet()) {
			var subResult = new ArrayList<String>();
			var presets = JsonUtils.getAsJsonArray(schedules.get(componentId), "presets");
			if (presets.size() != NUMBER_OF_HOURS && presets.size() != NUMBER_OF_QUARTERS) {
				throw new OpenemsException("[" + componentId + "] Got [" + presets.size() + "] presets. " //
						+ "Count must be exactly " //
						+ NUMBER_OF_HOURS + " (for hourly schedule) or " //
						+ NUMBER_OF_QUARTERS + "(for quarterly schedule)");
			}
			for (var preset : presets) {
				subResult.add(JsonUtils.getAsString(preset));
			}
			result.put(componentId, subResult.toArray(String[]::new));
		}
		return result.build();
	}

	/**
	 * Creates a {@link Schedule} for a {@link Schedulable} Component from a String
	 * array of Presets, starting from now.
	 * 
	 * <p>
	 * Assumes hourly Schedule presets if 24 values are given; quarterly Schedule
	 * otherwise.
	 * 
	 * @param now              the current {@link ZonedDateTime}
	 * @param stringPresets    String array of Presets; length is either 24 or 96
	 * @param componentPresets available {@link Preset}s for the {@link Schedulable}
	 *                         Component
	 * @return a {@link Schedule}
	 */
	protected static Schedule<?, ?> toSchedule(ZonedDateTime now, String[] stringPresets, Preset[] componentPresets) {
		// Map Preset-String to Preset
		var componentPresetsMap = Stream.of(componentPresets) //
				.collect(Collectors.toMap(Preset::name, Function.identity()));

		// Map Presets in Schedule
		List<Preset> presets = Stream.of(stringPresets) //
				.map(preset -> componentPresetsMap.get(preset)) //
				.collect(Collectors.toCollection(ArrayList::new));

		// Duplicate list to have sufficient entries for skip
		presets.addAll(presets);

		// Apply Schedule
		final Schedule<?, ?> schedule;
		if (presets.size() == NUMBER_OF_HOURS * 2) {
			// Hourly schedule was given
			schedule = Schedule.ofHourly(now, presets.stream() //
					.skip(now.getHour()) //
					.limit(NUMBER_OF_HOURS) //
					.toArray(Preset[]::new));
		} else {
			// Quarterly schedule was given
			schedule = Schedule.ofQuarterly(now, presets.stream() //
					.skip(now.getHour() * 4 + now.getMinute() / 15) //
					.limit(NUMBER_OF_QUARTERS) //
					.toArray(Preset[]::new));
		}
		return schedule;
	}

}
