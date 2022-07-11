package io.openems.backend.edgewebsocket;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.SystemLog;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.AbstractWebsocketServer;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	private final Logger log = LoggerFactory.getLogger(WebsocketServer.class);

	private final EdgeWebsocketImpl parent;
	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	public WebsocketServer(EdgeWebsocketImpl parent, String name, int port, int poolSize, boolean debugMode) {
		super(name, port, poolSize, debugMode);
		this.parent = parent;
		this.onOpen = new OnOpen(parent);
		this.onRequest = new OnRequest(parent);
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError(parent);
		this.onClose = new OnClose(parent);
	}

	@Override
	protected WsData createWsData() {
		return new WsData(this);
	}

	/**
	 * Is the given Edge online?.
	 *
	 * @param edgeId the Edge-ID
	 * @return true if it is online.
	 */
	public boolean isOnline(String edgeId) {
		final Optional<String> edgeIdOpt = Optional.of(edgeId);
		return this.getConnections().parallelStream().anyMatch(
				ws -> ws.getAttachment() != null && ((WsData) ws.getAttachment()).getEdgeId().equals(edgeIdOpt));
	}

	@Override
	protected OnOpen getOnOpen() {
		return this.onOpen;
	}

	@Override
	protected OnRequest getOnRequest() {
		return this.onRequest;
	}

	@Override
	public OnNotification getOnNotification() {
		return this.onNotification;
	}

	@Override
	protected OnError getOnError() {
		return this.onError;
	}

	@Override
	protected OnClose getOnClose() {
		return this.onClose;
	}

	@Override
	protected JsonrpcMessage handleNonJsonrpcMessage(String stringMessage, OpenemsNamedException lastException)
			throws OpenemsNamedException {
		var message = JsonUtils.parseToJsonObject(stringMessage);

		// config
		if (message.has("config")) {
			var config = EdgeConfig.fromJson(JsonUtils.getAsJsonObject(message, "config"));
			return new EdgeConfigNotification(config);
		}

		// timedata
		if (message.has("timedata")) {
			var d = new TimestampedDataNotification();
			var timedata = JsonUtils.getAsJsonObject(message, "timedata");
			for (Entry<String, JsonElement> entry : timedata.entrySet()) {
				var timestamp = Long.parseLong(entry.getKey());
				var values = JsonUtils.getAsJsonObject(entry.getValue());
				Map<ChannelAddress, JsonElement> data = new HashMap<>();
				for (Entry<String, JsonElement> value : values.entrySet()) {
					var address = ChannelAddress.fromString(value.getKey());
					data.put(address, value.getValue());
				}
				d.add(timestamp, data);
			}
			return d;
		}

		// log
		if (message.has("log")) {
			var log = JsonUtils.getAsJsonObject(message, "log");
			return new SystemLogNotification(new SystemLog(
					ZonedDateTime.ofInstant(Instant.ofEpochMilli(JsonUtils.getAsLong(log, "time")),
							ZoneId.systemDefault()), //
					SystemLog.Level.valueOf(JsonUtils.getAsString(log, "level").toUpperCase()), //
					JsonUtils.getAsString(log, "source"), //
					JsonUtils.getAsString(log, "message")));
		}

		this.log.info("EdgeWs. handleNonJsonrpcMessage: " + stringMessage);
		throw new OpenemsException("EdgeWs. handleNonJsonrpcMessage", lastException);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		this.parent.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		this.parent.logWarn(log, message);
	}

	@Override
	protected ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
			TimeUnit unit) {
		return super.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}
}
