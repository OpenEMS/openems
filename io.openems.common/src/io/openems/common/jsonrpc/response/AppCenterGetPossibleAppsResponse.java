package io.openems.common.jsonrpc.response;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for getting all possible apps to a key.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "bundles": {@link Bundle}[]
 *   }
 * }
 * </pre>
 */
public class AppCenterGetPossibleAppsResponse extends JsonrpcResponseSuccess {

	public final List<Bundle> possibleApps;

	/**
	 * Creates a {@link AppCenterGetPossibleAppsResponse} from a
	 * {@link JsonrpcResponseSuccess}.
	 *
	 * @param r the {@link JsonrpcResponseSuccess}
	 * @return a {@link AppCenterGetPossibleAppsResponse}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterGetPossibleAppsResponse from(JsonrpcResponseSuccess r) throws OpenemsNamedException {
		final var result = r.getResult();
		final var bundles = JsonUtils.getAsJsonArray(result, "bundles");
		return from(r.getId(), bundles);
	}

	/**
	 * Creates a {@link AppCenterGetPossibleAppsResponse} from a {@link List} of
	 * bundles of which every bundle has a {@link List} of {@link App}.
	 *
	 * @param id      id of the request
	 * @param bundles the bundles
	 * @return the {@link AppCenterGetPossibleAppsResponse}
	 */
	public static final AppCenterGetPossibleAppsResponse from(UUID id, JsonArray bundles) {
		return new AppCenterGetPossibleAppsResponse(id, //
				JsonUtils.stream(bundles) //
						.map(i -> i.getAsJsonArray()) //
						.map(Bundle::from) //
						.collect(Collectors.toList()) //
		);
	}

	public AppCenterGetPossibleAppsResponse(UUID id, List<Bundle> possibleApps) {
		super(id);
		this.possibleApps = possibleApps;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("bundles", this.possibleApps.stream() //
						.map(Bundle::toJsonArray) //
						.collect(JsonUtils.toJsonArray())) //
				.build();
	}

	public static final class App {
		public final String appId;

		/**
		 * Creates a {@link App} from an {@link JsonObject}.
		 *
		 * @param jsonObject the {@link JsonObject} to parse
		 * @return the {@link App}
		 */
		public static final App from(JsonObject jsonObject) {
			return new App(jsonObject.get("appId").getAsString());
		}

		public App(String appId) {
			this.appId = appId;
		}

		/**
		 * Creates a {@link JsonObject} of this object.
		 *
		 * @return the {@link JsonObject}
		 */
		public final JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.addProperty("appId", this.appId) //
					.build();
		}
	}

	public static final class Bundle {
		public final List<App> apps;

		/**
		 * Creates a {@link Bundle} from an {@link JsonArray}.
		 *
		 * @param jsonArray the {@link JsonArray} to parse
		 * @return the {@link Bundle}
		 */
		public static final Bundle from(JsonArray jsonArray) {
			return new Bundle(JsonUtils.stream(jsonArray) //
					.map(t -> t.getAsJsonObject()) //
					.map(App::from) //
					.collect(Collectors.toList()) //
			);
		}

		public Bundle(List<App> apps) {
			this.apps = apps;
		}

		/**
		 * Creates a {@link JsonArray} of this object.
		 *
		 * @return the {@link JsonArray}
		 */
		public final JsonArray toJsonArray() {
			return this.apps.stream() //
					.map(App::toJsonObject) //
					.collect(JsonUtils.toJsonArray());
		}
	}

}
