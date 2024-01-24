package io.openems.edge.bridge.http;

import java.util.concurrent.CompletableFuture;

public interface UrlFetcher {

	/**
	 * Creates a {@link Runnable} to execute a request with the given parameters.
	 * 
	 * @param urlString      the url to fetch
	 * @param connectTimeout the connection timeout
	 * @param readTimeout    the read timeout
	 * @param future         the {@link CompletableFuture} to fulfill after the
	 *                       fetch
	 * @return the {@link Runnable} to run to execute the fetch
	 */
	public Runnable createTask(//
			String urlString, //
			int connectTimeout, //
			int readTimeout, //
			CompletableFuture<String> future //
	);

}
