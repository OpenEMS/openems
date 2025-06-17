package io.openems.backend.common.edge.jsonrpc;

import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.stream;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class ConnectedEdges {

	private ConnectedEdges() {
	}

	public static class Notification extends JsonrpcNotification {

		public static final String METHOD = "connectedEdges";

		/**
		 * Create {@link ConnectedEdges.Notification} from a template
		 * {@link JsonrpcRequest}.
		 *
		 * @param n the template {@link JsonrpcNotification}
		 * @return the {@link ConnectedEdges.Notification}
		 * @throws OpenemsNamedException on parse error
		 */
		public static Notification from(JsonrpcNotification n) throws OpenemsNamedException {
			var p = n.getParams();
			var edgeIds = stream(getAsJsonArray(p, "edgeIds")) //
					.map(id -> id.getAsString()) //
					.collect(toSet());
			var metrics = getAsJsonObject(p, "metrics").entrySet().stream() //
					.collect(toMap(Entry::getKey, e -> e.getValue().getAsNumber()));
			return new Notification(edgeIds, metrics);
		}

		private final Set<String> edgeIds;
		private final Map<String, Number> metrics;

		public Notification(Set<String> edgeIds, Map<String, Number> metrics) {
			super(METHOD);
			this.edgeIds = edgeIds;
			this.metrics = metrics;
		}

		public Set<String> getEdgeIds() {
			return this.edgeIds;
		}

		public Map<String, Number> getMetrics() {
			return this.metrics;
		}

		@Override
		public JsonObject getParams() {
			return JsonUtils.buildJsonObject() //
					.add("edgeIds", this.edgeIds.stream() //
							.map(id -> new JsonPrimitive(id)) //
							.collect(JsonUtils.toJsonArray())) //
					.add("metrics", this.metrics.entrySet().stream() //
							.collect(JsonUtils.toJsonObject(//
									Entry::getKey, //
									e -> new JsonPrimitive(e.getValue())))) //
					.build();
		}
	}
}
