package io.openems.impl.api.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.WebSocketStream;

public class WebSocketClient extends AbstractVerticle {

	private static Logger log = LoggerFactory.getLogger(WebSocketClient.class);

	@Override public void start() throws Exception {
		Thread.sleep(1000);

		HttpClient client = vertx.createHttpClient(new HttpClientOptions());
		WebSocketStream stream = client.websocketStream(8090, "localhost", "/the_uri");
		stream.toObservable().subscribe(ws -> {
			{
				JsonObject jMessage = new JsonObject() //
						.put("subscription",
								new JsonObject() //
										.put("add",
												new JsonObject() //
														.put("ess0", //
																new JsonArray() //
																		.add("Soc"))));
				ws.writeFinalTextFrame(jMessage.encode());
			}

			ws.handler(message -> {
				JsonObject jMessage = new JsonObject(message.toString());
				log.info("Client received " + jMessage);
			});
		}, error -> {
			// Could not connect
		});
	}
}
