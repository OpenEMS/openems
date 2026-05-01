package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.utils.JsonUtils;

public record DependencyProperties(List<DependencyProperty> values) {

	/**
	 * Creates a {@link DependencyProperties} from a {@link JsonObject}.
	 *
	 * @param json the {@link JsonObject}
	 * @return the {@link DependencyProperties}
	 */
	public static DependencyProperties fromJson(JsonObject json) {
		return new DependencyProperties(//
				json.entrySet().stream()//
						.map(entry -> new DependencyProperty(entry.getKey(), entry.getValue()))//
						.toList());
	}

	/**
	 * Creates a {@link DependencyProperties} from a {@link JsonObject} with
	 * properties that should be updated at start.
	 *
	 * @param json                       the {@link JsonObject}
	 * @param propertyNamesToForceUpdate the name of the properties that should be
	 *                                   updated
	 * @return the {@link DependencyProperties}
	 */
	public static DependencyProperties fromJson(JsonObject json, String... propertyNamesToForceUpdate) {
		return new DependencyProperties(//
				json.entrySet() //
						.stream() //
						.map(entry -> getPropertyFrom(entry, propertyNamesToForceUpdate)) //
						.toList());
	}

	/**
	 * Returns an empty list of {@link DependencyProperties}.
	 * 
	 * @return the empty list
	 */
	public static DependencyProperties emptyProperties() {
		return new DependencyProperties(Collections.emptyList());
	}

	public DependencyProperties(List<DependencyProperty> values) {
		// TODO should be immutable list
		this.values = new ArrayList<>(values);
	}

	/**
	 * Creates a {@link JsonObject} from this object.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJson() {
		return this.values.stream() //
				.collect(JsonUtils.toJsonObject(t -> t.name, t -> t.value));
	}

	/**
	 * Adds a {@link DependencyProperty} to the list.
	 *
	 * @param name  the name of the property
	 * @param value the value of the property
	 */
	public void add(String name, JsonElement value) {
		this.values.add(new DependencyProperty(name, value));
	}

	/**
	 * Removes a {@link DependencyProperties} from the list.
	 * 
	 * @param name the name of the {@link DependencyProperties} that should be
	 *             removed
	 */
	public void remove(String name) {
		this.values.removeIf(p -> p.name.equals(name));
	}

	/**
	 * Checks whether a {@link DependencyProperty} with the specified name exists in
	 * the list.
	 *
	 * @param name the name of the property to search for
	 * @return true if a property with the given name exists, false otherwise
	 */
	public boolean has(String name) {
		return this.values.stream().anyMatch(prop -> prop.name.equals(name));
	}

	/**
	 * Returns the value of the given name.
	 *
	 * @param name the name of the property, the value should be found
	 * @return the value of the given name, returns null if not found
	 */
	public JsonElement get(String name) {
		return this.values.stream() //
				.filter(prop -> prop.name.equals(name)) //
				.map(prop -> prop.value) //
				.findAny() //
				.orElse(null);
	}

	/**
	 * Creates a deep copy of this instance.
	 *
	 * @return a new {@link DependencyProperties} instance with deep copied values
	 */
	public DependencyProperties deepCopy() {
		return new DependencyProperties(this.values);
	}

	private static DependencyProperty getPropertyFrom(Map.Entry<String, JsonElement> entry,
			String... propertyNamesToForceUpdate) {
		boolean forcedToUpdate = Arrays.stream(propertyNamesToForceUpdate)
				.anyMatch(name -> name.equals(entry.getKey()));
		return new DependencyProperty(entry.getKey(), entry.getValue(), ComponentProperties.Priority.required(),
				forcedToUpdate);
	}

	public record DependencyProperty(String name, JsonElement value, ComponentProperties.Priority priority,
			boolean forceUpdate) {

		public DependencyProperty {
			Objects.requireNonNull(name, "name is null");
			Objects.requireNonNull(value, "value is null");
		}

		public DependencyProperty(String name, JsonElement value) {
			this(name, value, ComponentProperties.Priority.required(), false);
		}

		/**
		 * Creates a {@link DependencyProperty} with its given name and the value null.
		 * 
		 * @param name the name of the DependencyProperty
		 * @return the {@link DependencyProperty}
		 */
		public static DependencyProperty of(String name) {
			return new DependencyProperty(name, JsonNull.INSTANCE);
		}

		/**
		 * Creates a copy of the current Property with the new name.
		 *
		 * @param name the new name
		 * @return the new property
		 */
		public DependencyProperty withName(String name) {
			return new DependencyProperty(name, this.value, this.priority, this.forceUpdate);
		}

		/**
		 * Creates a copy of the current Property with the new value.
		 *
		 * @param value the new value
		 * @return the new property
		 */
		public DependencyProperty withValue(JsonElement value) {
			return new DependencyProperty(this.name, value, this.priority, this.forceUpdate);
		}

		/**
		 * Creates a copy of the current Property with the new value.
		 *
		 * @param value the new value
		 * @return the new property
		 */
		public DependencyProperty withValue(boolean value) {
			return this.withValue(new JsonPrimitive(value));
		}

		/**
		 * Creates a copy of the current Property with the new value.
		 *
		 * @param value the new value
		 * @return the new property
		 */
		public DependencyProperty withValue(int value) {
			return this.withValue(new JsonPrimitive(value));
		}

		/**
		 * Creates a copy of the current Property with the new value.
		 *
		 * @param value the new value
		 * @return the new property
		 */
		public DependencyProperty withValue(long value) {
			return this.withValue(new JsonPrimitive(value));
		}

		/**
		 * Creates a copy of the current Property with the new value.
		 *
		 * @param value the new value
		 * @return the new property
		 */
		public DependencyProperty withValue(double value) {
			return this.withValue(new JsonPrimitive(value));
		}

		/**
		 * Creates a copy of the current Property with the new value.
		 *
		 * @param value the new value
		 * @return the new property
		 */
		public DependencyProperty withValue(String value) {
			return this.withValue(new JsonPrimitive(value));
		}

		/**
		 * Creates a copy of the current Property with the new priority.
		 *
		 * @param priority the new {@link ComponentProperties.Priority}
		 * @return the new property
		 */
		public DependencyProperty withPriority(ComponentProperties.Priority priority) {
			return new DependencyProperty(this.name, this.value, priority, this.forceUpdate);
		}

		/**
		 * Creates a copy of the current Property with the new priority order.
		 *
		 * @param priority the new priority
		 * @return the new property
		 */
		public DependencyProperty withPriority(int priority) {
			return this.withPriority(ComponentProperties.Priority.order(priority));
		}

		/**
		 * Creates a copy of the current Property with the new priority as required.
		 *
		 * @return the new property
		 */
		public DependencyProperty withPriorityRequired() {
			return this.withPriority(ComponentProperties.Priority.required());
		}

		/**
		 * Creates a copy of the current Property with the new force update value.
		 *
		 * @return the new property
		 */
		public DependencyProperty withForceUpdate(boolean forceUpdate) {
			return new DependencyProperty(this.name, this.value, this.priority, forceUpdate);
		}
	}
}
