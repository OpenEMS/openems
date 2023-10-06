package io.openems.edge.bridge.http;

import java.util.concurrent.CompletableFuture;

public interface UrlFetcher {

	public Runnable createTask(//
			String urlString, //
			int connectTimeout, //
			int readTimeout, //
			CompletableFuture<String> future //
	);

}
