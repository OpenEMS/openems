package io.openems.edge.core.appmanager.flag;

import com.google.gson.JsonElement;

import io.openems.common.utils.JsonUtils;

public interface Flag {

	/**
	 * Gets the name of the flag.
	 * 
	 * @return the name
	 */
	String name();

	/**
	 * Serializes this flag to a {@link JsonElement}.
	 * 
	 * @return the {@link JsonElement}
	 */
	default JsonElement toJson() {
		return JsonUtils.buildJsonObject() //
				.addProperty("name", this.name()) //
				.build();
	}

}
