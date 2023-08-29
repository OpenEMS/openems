package io.openems.edge.energy.api.schedulable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

/**
 * Holds Schedules for multiple Components.
 */
public class Schedules {

	private final ImmutableMap<String, Schedule<?, ?>> map;

	public static class Builder {
		private final Map<String, Schedule<?, ?>> map = new HashMap<>();

		private Builder() {
		}

		/**
		 * Add a {@link Schedule} for a Component.
		 * 
		 * @param componentId the Component-ID
		 * @param schedule    the {@link Schedule}
		 * @return builder
		 */
		public Builder add(String componentId, Schedule<?, ?> schedule) {
			this.map.put(componentId, schedule);
			return this;
		}

		public Schedules build() {
			return new Schedules(ImmutableMap.copyOf(this.map));
		}
	}

	/**
	 * Create a builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private Schedules(ImmutableMap<String, Schedule<?, ?>> elements) {
		this.map = elements;
	}

	/**
	 * Gets the {@link Schedule} for a Component.
	 * 
	 * @param <SCHEDULE>  the type of {@link Schedule}
	 * @param componentId the Component-ID
	 * @return the {@link Schedule}
	 */
	@SuppressWarnings("unchecked")
	public <SCHEDULE> SCHEDULE get(String componentId) {
		return (SCHEDULE) this.map.get(componentId);
	}

	/**
	 * Gets a debug log message.
	 * 
	 * @return a message
	 */
	public String debugLog() {
		return this.map.entrySet().stream() //
				.map(e -> {
					var preset = e.getValue().getCurrentPreset();
					return e.getKey() + ":" + (preset == null ? "-" : preset.name());
				}) //
				.collect(Collectors.joining("|"));
	}
}
