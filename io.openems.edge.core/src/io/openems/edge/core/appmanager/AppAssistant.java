package io.openems.edge.core.appmanager;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public class AppAssistant {

	/**
	 * A temporary builder class for an {@link AppAssistant}.
	 */
	public static class Builder implements Self<Builder>, OnlyIf<Builder> {

		private String name;
		private String alias;
		private JsonArray fields = new JsonArray();
		private List<AppConfigurationStep> steps = Collections.emptyList();

		protected Builder() {
		}

		/**
		 * Return the built {@link JsonArray}.
		 *
		 * @return the {@link JsonArray}
		 */
		public AppAssistant build() {
			return new AppAssistant(this.name, this.alias, this.fields, this.steps);
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

		/**
		 * Sets the App Configuration Steps.
		 *
		 * @param steps the steps
		 * @return the {@link Builder}
		 */
		public Builder steps(List<AppConfigurationStep> steps) {
			this.steps = steps;
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

	public record AppConfigurationStep(//
			String type, //
			JsonObject params //
	) {

		/**
		 * Creates a new OAuth Step for the App Configuration.
		 * 
		 * @return the created {@link OAuthAppConfigurationStep}
		 */
		public static OAuthAppConfigurationStep createOAuthStep() {
			return new OAuthAppConfigurationStep();
		}

		public static class OAuthAppConfigurationStep {
			public static final String TYPE = "oauth";

			private String oauthName;
			private Nameable componentIdPropertyPath;
			private String helperText;

			public OAuthAppConfigurationStep setOAuthName(String oauthName) {
				this.oauthName = oauthName;
				return this;
			}

			public OAuthAppConfigurationStep setComponentIdPropertyPath(Nameable componentIdPropertyPath) {
				this.componentIdPropertyPath = componentIdPropertyPath;
				return this;
			}

			public OAuthAppConfigurationStep setHelperText(String helperText) {
				this.helperText = helperText;
				return this;
			}

			public AppConfigurationStep build() {
				return new AppAssistant.AppConfigurationStep(OAuthAppConfigurationStep.TYPE, JsonUtils.buildJsonObject() //
						.addPropertyIfNotNull("oauthName", this.oauthName) //
						.onlyIf(this.componentIdPropertyPath != null, t -> t//
								.addProperty("componentIdPropertyPath", this.componentIdPropertyPath.name())) //
						.addPropertyIfNotNull("helperText", this.helperText) //
						.build());
			}

			private OAuthAppConfigurationStep() {
			}
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link AppConfigurationStep}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<AppConfigurationStep> serializer() {
			return jsonObjectSerializer(AppConfigurationStep.class, json -> {
				return new AppConfigurationStep(//
						json.getString("type"), //
						json.getJsonObject("params") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("type", obj.type) //
						.add("params", obj.params) //
						.build();
			});
		}

	}

	public final String name;
	public final String alias;
	public final JsonArray fields;
	public final List<AppConfigurationStep> steps;

	public AppAssistant(String name, String alias, JsonArray fields, List<AppConfigurationStep> steps) {
		this.name = name;
		this.alias = alias != null ? alias : name;
		this.fields = fields;
		this.steps = steps;
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link AppAssistant}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<AppAssistant> serializer() {
		return jsonObjectSerializer(AppAssistant.class, //
				json -> new AppAssistant(//
						json.getString("name"), //
						json.getString("alias"), //
						json.getJsonArray("fields"), //
						json.getList("steps", AppConfigurationStep.serializer()) //
				), //
				obj -> JsonUtils.buildJsonObject() //
						.addProperty("name", obj.name) //
						.addProperty("alias", obj.alias) //
						.add("fields", obj.fields) //
						.add("steps", obj.steps.stream() //
								.map(AppConfigurationStep.serializer()::serialize) //
								.collect(toJsonArray()))// ) //
						.build());
	}

}
