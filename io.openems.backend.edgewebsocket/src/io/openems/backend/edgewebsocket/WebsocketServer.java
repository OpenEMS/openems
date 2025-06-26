package io.openems.backend.edgewebsocket;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import com.google.gson.JsonElement;

import io.openems.common.types.ChannelAddress;
import io.openems.common.websocket.AbstractWebsocketServer;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	private final EdgeWebsocketImpl parent;
	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	public WebsocketServer(EdgeWebsocketImpl parent, String name, int port, int poolSize) {
		super(name, port, poolSize);
		this.parent = parent;
		this.onOpen = new OnOpen(parent);
		this.onRequest = new OnRequest(//
				() -> parent.appCenterMetadata, //
				this::logWarn);
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError(parent);
		this.onClose = new OnClose(parent);
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws);
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

	/**
	 * Gets the current cached date of the given edge and given channels.
	 * 
	 * @param edgeId   the id of the edge
	 * @param channels the channels
	 * @return the date
	 */
	public SortedMap<ChannelAddress, JsonElement> getCurrentDataFromEdgeCache(String edgeId,
			Set<ChannelAddress> channels) {
		record Pair<A, B>(A a, B b) {
		}

		return this.getConnections().stream() //
				.map(WebSocket::getAttachment) //
				.filter(Objects::nonNull) //
				.map(WsData.class::cast) //
				.filter(t -> t.getEdgeId().map(id -> id.equals(edgeId)).orElse(false)) //
				.map(w -> w.edgeCache) //
				.<Pair<ChannelAddress, JsonElement>>mapMulti((cache, consumer) -> {
					channels.stream() //
							.forEach(t -> {
								consumer.accept(new Pair<>(t, cache.getChannelValue(t.toString())));
							});
				}) //
				.collect(Collectors.toMap(Pair::a, Pair::b, (t, u) -> u, TreeMap::new));
	}

}
