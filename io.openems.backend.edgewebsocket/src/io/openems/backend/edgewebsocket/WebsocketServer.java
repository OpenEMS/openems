package io.openems.backend.edgewebsocket;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.SystemLog;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.AbstractWebsocketServer;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	private final EdgeWebsocketImpl parent;
	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	public WebsocketServer(EdgeWebsocketImpl parent, String name, int port, int poolSize, DebugMode debugMode) {
		super(name, port, poolSize, debugMode, (executor) -> {
		});
		this.parent = parent;
		this.onOpen = new OnOpen(parent);
		this.onRequest = new OnRequest(parent);
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError(parent);
		this.onClose = new OnClose(parent);
	}

	@Override
	protected WsData createWsData() {
		return new WsData();
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
	protected JsonrpcMessage handleNonJsonrpcMessage(WebSocket ws, String stringMessage,
			OpenemsNamedException lastException) throws OpenemsNamedException {
		var message = JsonUtils.parseToJsonObject(stringMessage);

		// config
		if (message.has("config")) {
			// Unable to handle deprecated configurations
			return null;
		}

		// timedata
		if (message.has("timedata")) {
			var d = new TimestampedDataNotification();
			var timedata = JsonUtils.getAsJsonObject(message, "timedata");
			for (Entry<String, JsonElement> entry : timedata.entrySet()) {
				var timestamp = Long.parseLong(entry.getKey());
				var values = JsonUtils.getAsJsonObject(entry.getValue());
				Map<String, JsonElement> data = new HashMap<>();
				for (Entry<String, JsonElement> value : values.entrySet()) {
					data.put(value.getKey(), value.getValue());
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
	protected void logError(Logger log, String message) {
		this.parent.logError(log, message);
	}
}
