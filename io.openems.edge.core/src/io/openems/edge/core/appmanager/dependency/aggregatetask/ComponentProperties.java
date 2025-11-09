package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
	 * Creates a {@link JsonObject} from a {@link ComponentProperties}.
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

	public record Property(String name, JsonElement value) {

	}

}