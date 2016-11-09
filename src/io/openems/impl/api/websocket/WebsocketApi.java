package io.openems.impl.api.websocket;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;

import io.openems.core.Databus;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.ServerWebSocket;

public class WebsocketApi extends AbstractVerticle {

	private static Logger log = LoggerFactory.getLogger(WebsocketApi.class);

	private final Databus databus;
	private final int port;

	public WebsocketApi(int port) {
		this.databus = Databus.getInstance();
		this.port = port;
	}

	@Override public void start(Future<Void> fut) throws Exception {

		// router.route("/ws/websocket/*").handler(request -> {
		// ServerWebSocket webSocket = request.request().upgrade();
		// logger.debug("New connection {}", webSocket.binaryHandlerID());
		// webSocket.handler(buffer -> {
		// logger.debug("Received: {} {} ", webSocket.binaryHandlerID(), new String(buffer.getBytes()));
		// JsonObject json = new JsonObject(new String(buffer.getBytes()));
		// json.put("senderId", webSocket.binaryHandlerID());
		// vertx.eventBus().publish("chat.broadcast", json);
		// });

		vertx.createHttpServer().websocketHandler(new Handler<ServerWebSocket>() {

			@Override public void handle(final ServerWebSocket ws) {
				log.info("Client connected. " + ws.path());

				/*
				 * Holds thingId and channelId, subscribed by this websocket
				 */
				HashMultimap<String, String> subscribedChannels = HashMultimap.create();

				/*
				 * Once every second, send data to client
				 */
				vertx.periodicStream(2000).toObservable().subscribe(id -> {
					JsonObject jMessage = new JsonObject();
					JsonObject jData = new JsonObject();
					subscribedChannels.keys().forEach(thingId -> {
						JsonObject jThingData = new JsonObject();
						subscribedChannels.get(thingId).forEach(channelId -> {
							Optional<?> value = databus.getValue(thingId, channelId);
							jThingData.put(channelId, value.orElse(null));
						});
						jData.put(thingId, jThingData);
					});
					jMessage.put("data", jData);
					ws.writeFinalTextFrame(jMessage.encode());
				});

				/*
				 * Receive data from client
				 */
				ws.handler(message -> {
					log.info("Message: " + message);
					JsonObject jMessage = new JsonObject(message.toString());
					log.info("Server received " + jMessage);

					/*
					 * Handle channel subscriptions
					 */
					JsonObject jSubscription = jMessage.getJsonObject("subscription");
					JsonObject jAdd = jSubscription.getJsonObject("add");
					jAdd.forEach(jThingEntry -> {
						String thingId = jThingEntry.getKey();
						JsonArray jChannelIds = (JsonArray) jThingEntry.getValue();
						jChannelIds.forEach(jChannelId -> {
							String channelId = jChannelId.toString();
							log.info("Add subscription for " + thingId + "/" + channelId);
							subscribedChannels.put(thingId, channelId);
						});
					});
				});
			}

		}).listen(this.port, result -> {
			if (result.succeeded()) {
				fut.complete();
			} else {
				fut.fail(result.cause());
			}
		});
	}
}
