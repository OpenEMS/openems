package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public record ComponentProperties(List<Property> values) {

	/**
	 * Returns a {@link ComponentProperties} with a empty list.
	 * 
	 * @return the {@link ComponentProperties}
	 */
	public static ComponentProperties emptyProperties() {
		return new ComponentProperties(Collections.emptyList());
	}

	/**
	 * Creates a {@link ComponentProperties} from a {@link Map}.
	 * 
	 * @param map the {@link Map}
	 * @return the {@link ComponentProperties}
	 */
	public static ComponentProperties fromMap(Map<String, JsonElement> map) {
		return new ComponentProperties(//
				map.entrySet() //
						.stream() //
						.map(entry -> new Property(entry.getKey(), entry.getValue())) //
						.collect(Collectors.toList()));
	}

	/**
	 * Creates a {@link ComponentProperties} from a {@link Map} with properties that
	 * should be updated at start.
	 *
	 * @param map           the {@link Map}
	 * @param propertyNames the name of the properties that should be updated
	 * @return the {@link ComponentProperties}
	 */
	public static ComponentProperties fromMap(Map<String, JsonElement> map, String... propertyNames) {
		return new ComponentProperties(//
				map.entrySet() //
						.stream() //
						.map(entry -> getPropertyFrom(entry, propertyNames)) //
						.collect(Collectors.toList()));
	}

	/**
	 * Creates a {@link ComponentProperties} from a {@link JsonObject}.
	 *
	 * @param json the {@link JsonObject}
	 * @return the {@link ComponentProperties}
	 */
	public static ComponentProperties fromJson(JsonObject json) {
		return new ComponentProperties(//
				json.entrySet().stream()//
						.map(entry -> new Property(entry.getKey(), entry.getValue()))//
						.toList());
	}

	/**
	 * Creates a {@link ComponentProperties} from a {@link JsonObject} with
	 * properties that should be updated at start.
	 *
	 * @param json          the {@link JsonObject}
	 * @param propertyNames the name of the properties that should be updated
	 * @return the {@link ComponentProperties}
	 */
	public static ComponentProperties fromJson(JsonObject json, String... propertyNames) {
		return new ComponentProperties(//
				json.entrySet() //
						.stream() //
						.map(entry -> getPropertyFrom(entry, propertyNames)) //
						.collect(Collectors.toList()));
	}

	/**
	 * Gets a single {@link Property}.
	 *
	 * @param name the {@link Property}
	 * @return the {@link Property}
	 */
	public Property getOrNull(String name) {
		return this.values().stream()//
				.filter(prop -> prop.name().equals(name))//
				.findFirst()//
				.orElse(null);
	}

	public record Property(String name, JsonElement value, Priority priority, boolean forceUpdate) {

		/**
		 * Creates a property of the name.
		 * 
		 * @param name the name of the property
		 * @return the created property
		 */
		public static Property of(String name) {
			return new Property(name, JsonNull.INSTANCE);
		}

		public Property(String name, JsonElement value) {
			this(name, value, Priority.required(), false);
		}

		/**
		 * Creates a copy of the current Property with the new name.
		 * 
		 * @param name the new name
		 * @return the new property
		 */
		public Property withName(String name) {
			return new Property(name, this.value, this.priority, this.forceUpdate);
		}

		/**
		 * Creates a copy of the current Property with the new value.
		 *
		 * @param value the new value
		 * @return the new property
		 */
		public Property withValue(JsonElement value) {
			return new Property(this.name, value, this.priority, this.forceUpdate);
		}

		/**
		 * Creates a copy of the current Property with the new value.
		 * 
		 * @param value the new value
		 * @return the new property
		 */
		public Property withValue(boolean value) {
			return this.withValue(new JsonPrimitive(value));
		}

		/**
		 * Creates a copy of the current Property with the new value.
		 *
		 * @param value the new value
		 * @return the new property
		 */
		public Property withValue(int value) {
			return this.withValue(new JsonPrimitive(value));
		}

		/**
		 * Creates a copy of the current Property with the new value.
		 *
		 * @param value the new value
		 * @return the new property
		 */
		public Property withValue(long value) {
			return this.withValue(new JsonPrimitive(value));
		}

		/**
		 * Creates a copy of the current Property with the new value.
		 *
		 * @param value the new value
		 * @return the new property
		 */
		public Property withValue(double value) {
			return this.withValue(new JsonPrimitive(value));
		}

		/**
		 * Creates a copy of the current Property with the new value.
		 *
		 * @param value the new value
		 * @return the new property
		 */
		public Property withValue(String value) {
			return this.withValue(new JsonPrimitive(value));
		}

		/**
		 * Creates a copy of the current Property with the new priority.
		 *
		 * @param priority the new {@link Priority}
		 * @return the new property
		 */
		public Property withPriority(Priority priority) {
			return new Property(this.name, this.value, priority, this.forceUpdate);
		}

		/**
		 * Creates a copy of the current Property with the new priority order.
		 *
		 * @param priority the new priority
		 * @return the new property
		 */
		public Property withPriority(int priority) {
			return this.withPriority(Priority.order(priority));
		}

		/**
		 * Creates a copy of the current Property with the new priority as required.
		 *
		 * @return the new property
		 */
		public Property withPriorityRequired() {
			return this.withPriority(Priority.required());
		}

		/**
		 * Creates a copy of the current Property with the new force update value.
		 *
		 * @return the new property
		 */
		public Property withForceUpdate(boolean forceUpdate) {
			return new Property(this.name, this.value, this.priority, forceUpdate);
		}

	}

	public sealed interface Priority extends Comparable<Priority> {

		/**
		 * Checks if the current priority is greater than the provided priority.
		 * 
		 * @param priority the other priority for comparison
		 * @return true if the current priority is greater than the provided priority
		 */
		default boolean isGreaterThan(Priority priority) {
			return this.compareTo(priority) > 0;
		}

		/**
		 * Checks if the current priority is lower than the provided priority.
		 *
		 * @param priority the other priority for comparison
		 * @return true if the current priority is lower than the provided priority
		 */
		default boolean isLowerThan(Priority priority) {
			return this.compareTo(priority) < 0;
		}

		/**
		 * Checks if the current priority is equal to the provided priority.
		 *
		 * @param priority the other priority for comparison
		 * @return true if the current priority is equal to the provided priority
		 */
		default boolean isSame(Priority priority) {
			return this.compareTo(priority) == 0;
		}

		@Override
		default int compareTo(Priority o) {
			if (this instanceof Required && o instanceof Required) {
				return 0;
			}
			if (this instanceof Required) {
				return 1;
			}
			if (o instanceof Required) {
				return -1;
			}
			final var intPriority = (IntPriority) this;
			final var intPriorityO = (IntPriority) o;
			return Integer.compare(intPriority.value(), intPriorityO.value());
		}

		/**
		 * Creates a {@link Priority} with the highest value.
		 *
		 * @return the priority
		 */
		static Priority required() {
			return Required.INSTANCE;
		}

		/**
		 * Creates a {@link Priority} of an integer where a higher value indicates a
		 * higher priority.
		 *
		 * @param priority the priority value
		 * @return the created {@link Priority}
		 */
		static Priority order(int priority) {
			return new IntPriority(priority);
		}

		final class Required implements Priority {

			public static final Required INSTANCE = new Required();

			private Required() {

			}
		}

		record IntPriority(int value) implements Priority {

			public IntPriority {
				if (value < 0) {
					throw new IllegalArgumentException();
				}
			}

		}

	}

	private static Property getPropertyFrom(Map.Entry<String, JsonElement> entry, String... propertyNames) {
		boolean forcedToUpdate = Arrays.stream(propertyNames).anyMatch(name -> name.equals(entry.getKey()));
		return new Property(entry.getKey(), entry.getValue(), Priority.required(), forcedToUpdate);
	}
}