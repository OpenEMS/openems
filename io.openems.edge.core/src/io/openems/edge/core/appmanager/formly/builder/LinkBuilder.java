package io.openems.edge.core.appmanager.formly.builder;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;

import java.util.UUID;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.jsonrpc.serialization.PolymorphicSerializer;
import io.openems.common.utils.JsonUtils;

public final class LinkBuilder extends FormlyBuilder<LinkBuilder> {

	public sealed interface Link {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Link}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Link> serializer() {
			final var polymorphicSerializer = PolymorphicSerializer.<Link>create() //
					.add(AppUpdateLink.class, AppUpdateLink.serializer(), AppUpdateLink.identifier()) //
					.build();
			return jsonSerializer(Link.class, json -> {
				return json.polymorphic(polymorphicSerializer,
						jsonElementPath -> jsonElementPath.getAsJsonObjectPath().getStringPath("type"));
			}, polymorphicSerializer::serialize);
		}

	}

	public record AppUpdateLink(String appId, UUID instanceId, String property) implements Link {

		public AppUpdateLink(String appId) {
			this(appId, null, null);
		}

		private static String identifier() {
			return "appUpdate";
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link AppUpdateLink}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<AppUpdateLink> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(AppUpdateLink.class, json -> {
				return new AppUpdateLink(json.getString("appId"), //
						json.getUuidOrNull("instanceId"), //
						json.getStringOrNull("property"));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("type", AppUpdateLink.identifier()) //
						.addProperty("appId", obj.appId) //
						.onlyIf(obj.instanceId() != null, b -> b.addProperty("instanceId", obj.instanceId().toString())) //
						.addPropertyIfNotNull("property", obj.property()) //
						.build();
			});
		}

	}

	public LinkBuilder() {
		// null because no key is needed if no input happens
		super(null);
	}

	public LinkBuilder setLink(Link link) {
		this.templateOptions.add("link", Link.serializer().serialize(link));
		return this;
	}

	@Override
	protected String getType() {
		return "link";
	}

}