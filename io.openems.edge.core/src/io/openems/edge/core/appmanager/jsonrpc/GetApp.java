package io.openems.edge.core.appmanager.jsonrpc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.flag.Flag;
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
public class GetApp {

	public static final String METHOD = "getApp";

	public static class Request extends JsonrpcRequest {

		/**
		 * Parses a generic {@link JsonrpcRequest} to a {@link Request}.
		 *
		 * @param r the {@link JsonrpcRequest}
		 * @return the {@link GetAppsRequest}
		 * @throws OpenemsNamedException on error
		 */
		public static Request from(JsonrpcRequest r) throws OpenemsNamedException {
			var p = r.getParams();
			var appId = JsonUtils.getAsString(p, "appId");
			return new Request(r, appId);
		}

		public final String appId;

		private Request(JsonrpcRequest request, String appId) {
			super(request, METHOD);
			this.appId = appId;
		}

		public Request(String appId) {
			super(METHOD);
			this.appId = appId;
		}

		@Override
		public JsonObject getParams() {
			return new JsonObject();
		}

	}

	public static class Response extends JsonrpcResponseSuccess {

		private final JsonObject app;

		public Response(UUID id, OpenemsApp app, List<OpenemsAppInstance> instantiatedApps, Language language,
				Validator validator) throws OpenemsNamedException {
			super(id);
			this.app = createJsonObjectOf(app, validator, instantiatedApps, language);
		}

		@Override
		public JsonObject getResult() {
			return JsonUtils.buildJsonObject() //
					.add("app", this.app) //
					.build();
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
