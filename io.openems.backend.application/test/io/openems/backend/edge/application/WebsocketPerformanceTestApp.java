package io.openems.backend.edge.application;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.EdgeConfig;

public class WebsocketPerformanceTestApp {

	/**
	 * Main.
	 * 
	 * @param args args
	 * @throws URISyntaxException   on error
	 * @throws InterruptedException on error
	 */
	public static void main(String[] args) throws URISyntaxException, InterruptedException {
		final var executor = Executors.newCachedThreadPool();

		for (var i = 0; i < 10000; i++) {
			var client = prepareTestClient(String.format("edge%d", i));
			executor.execute(() -> {
				client.sendMessage(new EdgeConfigNotification(EdgeConfig.empty()));
				for (var j = 0; j < 1000; j++) {
					sleep(1000);
					if (j % 100 == 0) {
						System.out.println(".");
					}
					client.sendMessage(buildDummyData());
				}
			});
		}
	}

	private static TestClient prepareTestClient(String edgeId) {
		try {
			Map<String, String> httpHeaders = new HashMap<>();
			httpHeaders.put("apikey", edgeId);
			var client = new TestClient(new URI("ws://localhost:8081"), httpHeaders);
			client.startBlocking();
			return client;

		} catch (URISyntaxException | InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static TimestampedDataNotification buildDummyData() {
		var timestamp = Instant.now().toEpochMilli();
		var table = TreeBasedTable.<Long, String, JsonElement>create();
		for (var i = 0; i < 100; i++) {
			table.put(timestamp, String.format("channel%d", i), new JsonPrimitive(i));
		}
		return new TimestampedDataNotification(table);
	}
}
