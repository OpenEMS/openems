package io.openems.edge.common.update.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.List;

import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.PolymorphicSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.update.jsonrpc.GetUpdateState.Request;
import io.openems.edge.common.update.jsonrpc.GetUpdateState.Response;

public class GetUpdateState implements EndpointRequestType<Request, Response> {

	public sealed interface UpdateState {

		public record Running(//
				int percentCompleted, //
				List<String> logs //
		) implements UpdateState {

			/**
			 * Gets the type identifier of this class used for serialization.
			 * 
			 * @return the type identifier
			 */
			public static String getTypeName() {
				return "running";
			}

			/**
			 * Returns a {@link JsonSerializer} for a
			 * {@link GetUpdateState.UpdateState.Running}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<GetUpdateState.UpdateState.Running> serializer() {
				return jsonObjectSerializer(GetUpdateState.UpdateState.Running.class, json -> {
					return new GetUpdateState.UpdateState.Running(//
							json.getInt("percentCompleted"), //
							json.getList("logs", JsonElementPath::getAsString) //
					);
				}, obj -> {
					return JsonUtils.buildJsonObject() //
							.addProperty("type", Running.getTypeName()) //
							.addProperty("percentCompleted", obj.percentCompleted()) //
							.add("logs", obj.logs().stream() //
									.map(JsonPrimitive::new) //
									.collect(toJsonArray()))
							.build();
				});
			}

		}

		public record Available(//
				String currentVersion, //
				String latestVersion //
		) implements UpdateState {

			/**
			 * Gets the type identifier of this class used for serialization.
			 * 
			 * @return the type identifier
			 */
			public static String getTypeName() {
				return "available";
			}

			/**
			 * Returns a {@link JsonSerializer} for a
			 * {@link GetUpdateState.UpdateState.Available}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<GetUpdateState.UpdateState.Available> serializer() {
				return jsonObjectSerializer(GetUpdateState.UpdateState.Available.class, json -> {
					return new GetUpdateState.UpdateState.Available(//
							json.getString("currentVersion"), //
							json.getString("latestVersion") //
					);
				}, obj -> {
					return JsonUtils.buildJsonObject() //
							.addProperty("type", Available.getTypeName()) //
							.addProperty("currentVersion", obj.currentVersion()) //
							.addProperty("latestVersion", obj.latestVersion()) //
							.build();
				});
			}

		}

		public record Updated(String version) implements UpdateState {

			/**
			 * Gets the type identifier of this class used for serialization.
			 * 
			 * @return the type identifier
			 */
			public static String getTypeName() {
				return "updated";
			}

			/**
			 * Returns a {@link JsonSerializer} for a
			 * {@link GetUpdateState.UpdateState.Updated}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<GetUpdateState.UpdateState.Updated> serializer() {
				return jsonObjectSerializer(GetUpdateState.UpdateState.Updated.class, //
						json -> new GetUpdateState.UpdateState.Updated(//
								json.getString("version")), //
						obj -> JsonUtils.buildJsonObject() //
								.addProperty("type", Updated.getTypeName()) //
								.addProperty("version", obj.version()) //
								.build());
			}

		}

		public record Unknown() implements UpdateState {

			/**
			 * Gets the type identifier of this class used for serialization.
			 * 
			 * @return the type identifier
			 */
			public static String getTypeName() {
				return "unknown";
			}

			/**
			 * Returns a {@link JsonSerializer} for a
			 * {@link GetUpdateState.UpdateState.Unknown}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<GetUpdateState.UpdateState.Unknown> serializer() {
				return jsonObjectSerializer(GetUpdateState.UpdateState.Unknown.class, //
						json -> new GetUpdateState.UpdateState.Unknown(), //
						obj -> JsonUtils.buildJsonObject() //
								.addProperty("type", Unknown.getTypeName()) //
								.build());
			}

		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetUpdateState.UpdateState}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetUpdateState.UpdateState> serializer() {
			final var polymorphicSerializer = PolymorphicSerializer.<GetUpdateState.UpdateState>create() //
					.add(Running.class, Running.serializer(), Running.getTypeName()) //
					.add(Available.class, Available.serializer(), Available.getTypeName()) //
					.add(Updated.class, Updated.serializer(), Updated.getTypeName()) //
					.add(Unknown.class, Unknown.serializer(), Unknown.getTypeName())//
					.build();

			return jsonSerializer(GetUpdateState.UpdateState.class, json -> {
				return json.polymorphic(polymorphicSerializer, t -> t.getAsJsonObjectPath().getStringPath("type"));
			}, obj -> {
				return polymorphicSerializer.serialize(obj);
			});
		}

	}

	@Override
	public String getMethod() {
		return "getUpdateState";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request(String id) {
		/**
		 * Returns a {@link JsonSerializer} for a {@link GetUpdateState.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetUpdateState.Request> serializer() {
			return jsonObjectSerializer(json -> {
				return new GetUpdateState.Request(json.getString("id"));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("id", obj.id()) //
						.build();
			});
		}

	}

	public record Response(UpdateState state) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetUpdateState.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetUpdateState.Response> serializer() {
			return jsonObjectSerializer(json -> {
				return new GetUpdateState.Response(json.getObject("state", UpdateState.serializer()));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("state", UpdateState.serializer().serialize(obj.state())) //
						.build();
			});
		}

	}

}
