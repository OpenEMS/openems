package io.openems.edge.core.appmanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

public class AppAssistant {

	/**
	 * A temporary builder class for an {@link AppAssistant}.
	 */
	public static class Builder implements Self<Builder>, OnlyIf<Builder> {

		private String name;
		private String alias;
		private JsonArray fields = new JsonArray();

		protected Builder() {
		}

		/**
		 * Return the built {@link JsonArray}.
		 *
		 * @return the {@link JsonArray}
		 */
		public AppAssistant build() {
			return new AppAssistant(this.name, this.alias, this.fields);
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
		 * Sets the alias.
		 *
		 * @param alias the alias
		 * @return the {@link Builder}
		 */
		public Builder setAlias(String alias) {
			this.alias = alias;
			return this;
		}

		/**
		 * Sets the App name.
		 *
		 * @param name the app name
		 * @return the {@link Builder}
		 */
		public Builder setAppName(String name) {
			this.name = name;
			return this;
		}

		@Override
		public Builder self() {
			return this;
		}

	}

	/**
	 * Creates an {@link AppAssistant} using a Builder.
	 *
	 * @param appname the name of the app
	 * @return the {@link Builder}
	 */
	public static Builder create(String appname) {
		return new Builder().setAppName(appname);
	}

	public final String name;
	public final String alias;
	public final JsonArray fields;

	private AppAssistant(String name, String alias, JsonArray fields) {
		this.name = name;
		this.alias = alias != null ? alias : name;
		this.fields = fields;
	}

	/**
	 * Gets this {@link AppAssistant} as {@link JsonObject}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addProperty("name", this.name) //
				.addProperty("alias", this.alias) //
				.add("fields", this.fields) //
				.build();
	}

}
