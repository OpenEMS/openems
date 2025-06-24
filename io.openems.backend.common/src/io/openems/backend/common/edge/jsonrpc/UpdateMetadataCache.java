package io.openems.backend.common.edge.jsonrpc;

import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.toJson;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class UpdateMetadataCache {

	private UpdateMetadataCache() {
	}

	public static class Notification extends JsonrpcNotification {

		public static final String METHOD = "updateMetadataCache";

		/**
		 * Create {@link UpdateMetadataCache.Notification} from a template
		 * {@link JsonrpcRequest}.
		 *
		 * @param n the template {@link JsonrpcNotification}
		 * @return the {@link UpdateMetadataCache.Notification}
		 * @throws OpenemsNamedException on parse error
		 */
		public static Notification from(JsonrpcNotification n) throws OpenemsNamedException {
			var p = n.getParams();
			var apikeysToEdgeIds = getAsJsonObject(p, "apikeysToEdgeIds").entrySet().stream() //
					.collect(toMap(Entry::getKey, e -> e.getValue().getAsString()));
			return new Notification(apikeysToEdgeIds);
		}

		/**
		 * Create empty {@link UpdateMetadataCache.Notification}.
		 * 
		 * @return the {@link UpdateMetadataCache.Notification}
		 */
		public static Notification empty() {
			return new Notification(Map.of());
		}

		private final Map<String, String> apikeysToEdgeIds;

		public Notification(Map<String, String> apikeysToEdgeIds) {
			super(METHOD);
			this.apikeysToEdgeIds = apikeysToEdgeIds;
		}

		public Map<String, String> getApikeysToEdgeIds() {
			return this.apikeysToEdgeIds;
		}

		@Override
		public JsonObject getParams() {
			return JsonUtils.buildJsonObject() //
					.add("apikeysToEdgeIds", this.apikeysToEdgeIds.entrySet().stream() //
							.collect(JsonUtils.toJsonObject(Entry::getKey, e -> toJson(e.getValue())))) //
					.build();
		}
	}
}
