package io.openems.edge.core.appmanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

public class AppAssistant {

	/**
	 * Creates an {@link AppAssistant} using a Builder.
	 *
	 * @return the {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * A temporary builder class for an {@link AppAssistant}.
	 */
	public static class Builder {

		private JsonArray fields = new JsonArray();

		protected Builder() {
		}

		/**
		 * Sets the Fields.
		 *
		 * @param fields the fields
		 * @return the {@link Builder}
		 */
		public Builder fields(JsonArray fields) {
			this.fields = fields;
			return this;
		}

		/**
		 * Return the built {@link JsonArray}.
		 *
		 * @return the {@link JsonArray}
		 */
		public AppAssistant build() {
			return new AppAssistant(this.fields);
		}

	}

	private final JsonArray fields;

	private AppAssistant(JsonArray fields) {
		this.fields = fields;
	}

	/**
	 * Gets this {@link AppAssistant} as {@link JsonObject}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.add("fields", this.fields) //
				.build();
	}

}
