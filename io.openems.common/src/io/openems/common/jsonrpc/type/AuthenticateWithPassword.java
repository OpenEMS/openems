package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.List;
import java.util.Objects;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.AuthenticateWithPassword.Request;
import io.openems.common.jsonrpc.type.AuthenticateWithPassword.Response;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

public class AuthenticateWithPassword implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "authenticateWithPassword";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request(//
			String username, // null-able
			String password //
	) {

		public Request {
			Objects.requireNonNull(password);
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link AuthenticateWithPassword}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, json -> {
				return new Request(//
						json.getStringOrNull("username"), //
						json.getString("password") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addPropertyIfNotNull("username", obj.username()) //
						.addProperty("password", obj.password()) //
						.build();
			});
		}

	}

	public record Response(//
			String token, //
			UserMetadata user, //
			List<EdgeMetadata> edges //
	) {

		public record UserMetadata(//
				String id, //
				String name, //
				Language language, //
				boolean hasMultipleEdges, //
				JsonObject settings, //
				Role globalRole //
		) {

			/**
			 * Returns a {@link JsonSerializer} for a
			 * {@link AuthenticateWithPassword.Response.UserMetadata}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<AuthenticateWithPassword.Response.UserMetadata> serializer() {
				return jsonObjectSerializer(AuthenticateWithPassword.Response.UserMetadata.class, json -> {
					return new AuthenticateWithPassword.Response.UserMetadata(//
							json.getString("id"), //
							json.getString("name"), //
							json.getEnum("language", Language.class), //
							json.getBoolean("hasMultipleEdges"), //
							json.getJsonObject("settings"), //
							json.getEnum("globalRole", Role.class) //
					);
				}, obj -> {
					return JsonUtils.buildJsonObject() //
							.addProperty("id", obj.id()) //
							.addProperty("name", obj.name()) //
							.addProperty("language", obj.language())//
							.addProperty("hasMultipleEdges", obj.hasMultipleEdges())//
							.add("settings", obj.settings()) //
							.addProperty("globalRole", obj.globalRole().name().toLowerCase()) //
							.build();
				});
			}

		}

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link AuthenticateWithPassword.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<AuthenticateWithPassword.Response> serializer() {
			return jsonObjectSerializer(Response.class, json -> {
				return new Response(//
						json.getString("token"), //
						json.getObject("user", UserMetadata.serializer()), //
						json.getList("edges", EdgeMetadata.serializer()) //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("token", obj.token()) //
						.add("user", UserMetadata.serializer().serialize(obj.user())) //
						.add("edges", EdgeMetadata.serializer().toListSerializer().serialize(obj.edges())) //
						.build();
			});
		}

	}

}
