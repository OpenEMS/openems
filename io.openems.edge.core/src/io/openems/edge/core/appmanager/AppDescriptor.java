package io.openems.edge.core.appmanager;

import static io.openems.common.utils.StringUtils.definedOrElse;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

public class AppDescriptor {

	public static final class AppDescriptorBuilder {

		private String websiteUrl = null;

		private AppDescriptorBuilder() {
		}

		public AppDescriptorBuilder setWebsiteUrl(String websiteUrl) {
			this.websiteUrl = definedOrElse(websiteUrl, null);
			return this;
		}

		public AppDescriptor build() {
			return new AppDescriptor(this.websiteUrl);
		}

	}

	private final String websiteUrl;

	private AppDescriptor(String websiteUrl) {
		this.websiteUrl = websiteUrl;
	}

	/**
	 * Creates a Builder for an {@link AppDescriptor}.
	 *
	 * @return the builder
	 */
	public static AppDescriptorBuilder create() {
		return new AppDescriptorBuilder();
	}

	public final String getWebsiteUrl() {
		return this.websiteUrl;
	}

	/**
	 * Builds a {@link JsonObject} out of this {@link AppDescriptor}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addPropertyIfNotNull("websiteUrl", this.websiteUrl) //
				.build();
	}

}
