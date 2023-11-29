package io.openems.edge.bridge.http;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingFunction;

public class DummyUrlFetcher implements UrlFetcher {

	private static final Runnable EMPTY_RUNNABLE = () -> {
		// empty
	};

	private final List<ThrowingFunction<String, String, OpenemsNamedException>> urlHandler = new LinkedList<>();
	private Runnable onTaskFinished = EMPTY_RUNNABLE;

	@Override
	public Runnable createTask(//
			final String urlString, //
			final int connectTimeout, //
			final int readTimeout, //
			final CompletableFuture<String> future //
	) {
		return () -> {
			try {
				for (var handler : this.urlHandler) {
					final var result = handler.apply(urlString);
					if (result != null) {
						future.complete(result);
						return;
					}
				}
			} catch (Throwable e) {
				future.completeExceptionally(e);
			} finally {
				this.onTaskFinished.run();
			}
		};
	}

	/**
	 * Adds a static handler for a fetch request.
	 * 
	 * @param handler the handler
	 */
	public void addUrlHandler(ThrowingFunction<String, String, OpenemsNamedException> handler) {
		this.urlHandler.add(handler);
	}

	public void setOnTaskFinished(Runnable onTaskFinished) {
		this.onTaskFinished = onTaskFinished == null ? EMPTY_RUNNABLE : onTaskFinished;
	}

}
