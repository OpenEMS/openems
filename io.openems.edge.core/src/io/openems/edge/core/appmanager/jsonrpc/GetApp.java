package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.flag.Flag;
import io.openems.edge.core.appmanager.jsonrpc.GetApp.Request;
import io.openems.edge.core.appmanager.jsonrpc.GetApp.Response;
import io.openems.edge.core.appmanager.validator.OpenemsAppStatus;
import io.openems.edge.core.appmanager.validator.Validator;

/**
 * Gets the available {@link OpenemsApp}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getApp",
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
 *     app: {
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
 *     }
 *   }
 * }
 * </pre>
 */
public class GetApp implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getApp";
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
			String appId //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetApp.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetApp.Request> serializer() {
			return jsonObjectSerializer(GetApp.Request.class, //
					json -> new GetApp.Request(//
							json.getString("appId")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("appId", obj.appId()) //
							.build());
		}

	}

	public record Response(//
			JsonObject app //
	) {

		/**
		 * Creates a Response.
		 * 
		 * @param app              the app of the response
		 * @param instantiatedApps all created {@link OpenemsAppInstance}
		 * @param language         the current language
		 * @param validator        the {@link Validator}
		 * @return the created Response
		 * @throws OpenemsNamedException on error
		 */
		public static Response newInstance(OpenemsApp app, List<OpenemsAppInstance> instantiatedApps, Language language,
				Validator validator) throws OpenemsNamedException {
			return new Response(createJsonObjectOf(app, validator, instantiatedApps, language));
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetApp.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetApp.Response> serializer() {
			return jsonObjectSerializer(GetApp.Response.class, //
					json -> new GetApp.Response(JsonUtils.buildJsonObject() //
							.addProperty("appId", json.getString("appId")) //
							.build()), //
					obj -> JsonUtils.buildJsonObject() //
							.add("app", obj.app()) //
							.build());
		}

	}

	/**
	 * Creates a {@link JsonObject} of the given {@link OpenemsApp}.
	 * 
	 * <p>
	 * Also adds the status of the app and the instances of the app.
	 * 
	 * @param app              the {@link OpenemsApp}
	 * @param validator        a {@link Validator} to get the status of the app
	 *                         {@link OpenemsAppStatus}
	 * @param instantiatedApps all instances
	 * @param language         the {@link Language}
	 * @return the created {@link JsonObject}
	 * @throws OpenemsNamedException on error
	 */
	public static final JsonObject createJsonObjectOf(//
			final OpenemsApp app, //
			final Validator validator, //
			final List<OpenemsAppInstance> instantiatedApps, //
			final Language language //
	) throws OpenemsNamedException {

		final var imageFuture = CompletableFuture.supplyAsync(app::getImage);
		final var statusFuture = CompletableFuture
				.supplyAsync(() -> validator.toJsonObject(app.getValidatorConfig(), language));
		try {
			final var image = imageFuture.get();
			final var status = statusFuture.get();

			return JsonUtils.buildJsonObject() //
					.add("categorys", Arrays.stream(app.getCategories()) //
							.map(cat -> cat.toJsonObject(language)) //
							.collect(JsonUtils.toJsonArray())) //
					.addProperty("cardinality", app.getCardinality().name()) //
					.addProperty("appId", app.getAppId()) //
					.addProperty("name", app.getName(language)) //
					.addProperty("shortName", app.getShortName(language)) //
					.addPropertyIfNotNull("image", image) //
					.add("flags", Arrays.stream(app.flags()) //
							.map(Flag::toJson) //
							.collect(JsonUtils.toJsonArray()))
					.add("status", status) //
					.add("instanceIds", instantiatedApps.stream() //
							.filter(instance -> app.getAppId().equals(instance.appId)) //
							.map(instance -> new JsonPrimitive(instance.instanceId.toString())) //
							.collect(JsonUtils.toJsonArray())) //
					.build();
		} catch (InterruptedException | CancellationException e) {
			throw new OpenemsException(e);
		} catch (ExecutionException e) {
			e.getCause().printStackTrace();
			throw new OpenemsException(e.getCause());
		}
	}

}
