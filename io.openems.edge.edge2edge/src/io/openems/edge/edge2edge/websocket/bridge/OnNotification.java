package io.openems.edge.edge2edge.websocket.bridge;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.ChannelUpdateNotification;
import io.openems.common.jsonrpc.notification.CurrentDataNotification;
import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserChannelAddress;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;

public class OnNotification implements io.openems.common.websocket.OnNotification {

	private final Logger log = LoggerFactory.getLogger(OnNotification.class);

	private final Consumer<Map<ChannelAddress, JsonElement>> onCurrentData;
	private final Consumer<EdgeConfig> onEdgeConfig;
	private final Runnable onChannelUpdate;

	public OnNotification(//
			Consumer<Map<ChannelAddress, JsonElement>> onCurrentData, //
			Consumer<EdgeConfig> onEdgeConfig, //
			Runnable onChannelUpdate //
	) {
		super();
		this.onCurrentData = onCurrentData;
		this.onEdgeConfig = onEdgeConfig;
		this.onChannelUpdate = onChannelUpdate;
	}

	@Override
	public void accept(WebSocket ws, JsonrpcNotification notification) throws OpenemsException {
		try {
			final var n = GenericJsonrpcNotification.from(notification.getParams().get("payload").getAsJsonObject());
			switch (n.getMethod()) {
			case EdgeConfigNotification.METHOD -> {
				final var edgeConfig = EdgeConfig.fromJson(n.getParams());
				this.onEdgeConfig.accept(edgeConfig);
			}
			case CurrentDataNotification.METHOD -> {
				final var data = currentDataSerializer().deserialize(n.getParams());
				this.onCurrentData.accept(data);
			}
			case ChannelUpdateNotification.METHOD -> {
				this.onChannelUpdate.run();
			}
			default -> {
				this.log.info("Unhandled Notification: " + JsonUtils.prettyToString(notification.getParams()));
			}
			}
		} catch (OpenemsNamedException e) {
			this.log.warn("Error while receiving notification", e);
		}

	}

	private static JsonSerializer<Map<ChannelAddress, JsonElement>> currentDataSerializer() {
		return jsonObjectSerializer(json -> {
			return json.collect(new StringParserChannelAddress(),
					toMap(t -> t.getKey().get(), t -> t.getValue().get()));
		}, obj -> {
			return obj.entrySet().stream() //
					.collect(JsonUtils.toJsonObject(t -> t.getKey().toString(), t -> t.getValue()));
		});
	}

}
