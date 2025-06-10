package io.openems.edge.core.appmanager;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.StringUtils.definedOrElse;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
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
	 * Returns a {@link JsonSerializer} for a {@link AppDescriptor}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<AppDescriptor> serializer() {
		return jsonObjectSerializer(AppDescriptor.class, json -> {
			return new AppDescriptor(//
					json.getString("websiteUrl") //
			);
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addPropertyIfNotNull("websiteUrl", obj.websiteUrl) //
					.build();
		});
	}

}
