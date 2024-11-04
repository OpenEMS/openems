package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.emptyObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.List;
import java.util.Objects;

import com.google.gson.JsonArray;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.GetApps.Request;
import io.openems.edge.core.appmanager.jsonrpc.GetApps.Response;
import io.openems.edge.core.appmanager.validator.Validator;

/**
 * Gets the available {@link OpenemsApp}s.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getApps",
 *   "params": {}
 * }
 * </pre>
 *
 * <p>
 * Response:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     apps: [{
 *       "categorys": [{
 *       	"name": string (OpenemsAppCategory enum),
 *       	"readableName": string
 *       }],
 *       "cardinality": string (OpenemsAppUsage enum),
 *       "appId": string,
 *       "name": string,
 *       "status": {
 *       	"status": string (OpenemsAppStatus enum),
 *       	"errorCompatibleMessages": string[],
 *       	"errorInstallableMessages": string[]
 *       },
 *       "image": string (base64),
 *       "instanceIds": UUID[],
 *     }]
 *   }
 * }
 * </pre>
 */
public class GetApps implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getApps";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request() {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetApps.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return emptyObjectSerializer(Request::new);
		}

	}

	public record Response(//
			JsonArray apps //
	) {

		/**
		 * Creates a new Response.
		 * 
		 * @param availableApps    all available app
		 * @param instantiatedApps all {@link OpenemsAppInstance}
		 * @param userRole         the current {@link Role} of the user
		 * @param language         the current {@link Language} of the user
		 * @param validator        the {@link Validator} to validate the app
		 * @return the created Response
		 */
		public static Response newInstance(List<OpenemsApp> availableApps, List<OpenemsAppInstance> instantiatedApps,
				Role userRole, Language language, Validator validator) {
			return new Response(createAppsArray(availableApps, instantiatedApps, userRole, language, validator));
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetApps.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, json -> {
				// TODO serialize whole apps not only JsonArray
				return new Response(json.getJsonArray("apps"));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("apps", obj.apps()) //
						.build();
			});
		}

		private static JsonArray createAppsArray(List<OpenemsApp> availableApps,
				List<OpenemsAppInstance> instantiatedApps, Role userRole, Language language, Validator validator) {
			return availableApps.stream() //
					.filter(app -> {
						final var permissions = app.getAppPermissions();
						if (!userRole.isAtLeast(permissions.canSee())) {
							return false;
						}
						return true;
					}) //
					.parallel() //
					.map(app -> {
						try {
							return GetApp.createJsonObjectOf(app, validator, instantiatedApps, language);
						} catch (OpenemsNamedException e) {
							e.printStackTrace();
							return null;
						}
					}) //
					.filter(Objects::nonNull) //
					.collect(JsonUtils.toJsonArray());
		}

	}

}
