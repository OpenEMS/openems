package io.openems.edge.common.oauth.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.List;

import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public class InitiateOAuthConnect implements EndpointRequestType<InitiateOAuthConnect.Request, InitiateOAuthConnect.Response> {

	@Override
	public String getMethod() {
		return "initiateConnect";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request(String identifier) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, json -> {
				return new Request(//
						json.getString("identifier") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("identifier", obj.identifier()) //
						.build();
			});
		}

	}

	public record Response(//
			String url, //
			String clientId, //
			String redirectUri, //
			List<String> scopes, //
			String state, //
			String codeChallenge, //
			String codeChallengeMethod //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, json -> {
				return new Response(//
						json.getString("url"), //
						json.getString("clientId"), //
						json.getString("redirectUri"), //
						json.getList("scopes", JsonElementPath::getAsString), //
						json.getString("state"), //
						json.getStringOrNull("codeChallenge"), //
						json.getStringOrNull("codeChallengeMethod") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("url", obj.url()) //
						.addProperty("clientId", obj.clientId()) //
						.addProperty("redirectUri", obj.redirectUri()) //
						.add("scopes", obj.scopes().stream() //
								.map(JsonPrimitive::new) //
								.collect(toJsonArray())) //
						.addProperty("state", obj.state()) //
						.addPropertyIfNotNull("codeChallenge", obj.codeChallenge()) //
						.addPropertyIfNotNull("codeChallengeMethod", obj.codeChallengeMethod()) //
						.build();
			});
		}

	}

}
