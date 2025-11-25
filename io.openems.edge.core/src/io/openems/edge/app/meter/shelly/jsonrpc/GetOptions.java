package io.openems.edge.app.meter.shelly.jsonrpc;

import java.util.List;
import java.util.UUID;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public final class GetOptions<T> implements EndpointRequestType<GetOptions.Request, GetOptions.Response<T>> {
	public static final String METHOD = "getOptions";

	private final JsonSerializer<T> valueSerializer;

	public GetOptions(JsonSerializer<T> valueSerializer) {
		this.valueSerializer = valueSerializer;
	}

	@Override
	public String getMethod() {
		return GetOptions.METHOD;
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response<T>> getResponseSerializer() {
		return Response.serializer(this.valueSerializer);
	}

	public record Request(UUID forInstance) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Request}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(Request.class, json -> {
				return new Request(json.getUuidOrNull("forInstance"));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.onlyIf(obj.forInstance() != null,
								t -> t.addProperty("forInstance", obj.forInstance().toString())) //
						.build();
			});
		}

	}

	public record Option<T>(String name, T value, String detail, OptionState state) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Option}.
		 *
		 * @param <T>             the type of the value
		 * @param valueSerializer the value {@link JsonSerializer}
		 * @return the created {@link JsonSerializer}
		 */
		public static <T> JsonSerializer<Option<T>> serializer(JsonSerializer<T> valueSerializer) {
			return JsonSerializerUtil.jsonObjectSerializer(json -> {
				return new Option<>(//
						json.getString("name"), //
						json.getObject("value", valueSerializer), //
						json.getStringOrNull("detail"), //
						json.getObjectOrNull("state", OptionState.serializer()) //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("name", obj.name()) //
						.add("value", valueSerializer.serialize(obj.value())) //
						.addPropertyIfNotNull("detail", obj.detail()) //
						.onlyIf(obj.state() != null,
								t -> t.add("state", OptionState.serializer().serialize(obj.state()))) //
						.build();
			});
		}

	}

	public record OptionState(boolean disabled, String text) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link OptionState}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<OptionState> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(OptionState.class, json -> {
				return new OptionState(json.getBoolean("disabled"), //
						json.getStringOrNull("text") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("disabled", obj.disabled()) //
						.addPropertyIfNotNull("text", obj.text()) //
						.build();
			});
		}

	}

	public record Response<T>(List<Option<T>> options) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Response}.
		 *
		 * @param <T>             the type of the value
		 * @param valueSerializer the value {@link JsonSerializer}
		 * @return the created {@link JsonSerializer}
		 */
		public static <T> JsonSerializer<Response<T>> serializer(JsonSerializer<T> valueSerializer) {
			return JsonSerializerUtil.jsonObjectSerializer(json -> {
				return new Response<>(//
						json.getList("options", Option.serializer(valueSerializer)) //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("options", Option.serializer(valueSerializer).toListSerializer().serialize(obj.options())) //
						.build();
			});
		}

	}
}