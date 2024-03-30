package io.openems.edge.bridge.http;

import java.util.LinkedList;
import java.util.List;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingFunction;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;

public class DummyUrlFetcher implements UrlFetcher {

	private static final Runnable EMPTY_RUNNABLE = () -> {
		// empty
	};

	private final List<ThrowingFunction<Endpoint, String, OpenemsNamedException>> urlHandler = new LinkedList<>();
	private Runnable onTaskFinished = EMPTY_RUNNABLE;

	@Override
	public String fetchEndpoint(//
			final Endpoint endpoint //
	) throws OpenemsNamedException {
		try {
			for (var handler : this.urlHandler) {
				final var result = handler.apply(endpoint);
				if (result != null) {
					return result;
				}
			}
			throw new OpenemsException("");
		} finally {
			this.onTaskFinished.run();
		}
	}

	/**
	 * Adds a static handler for a fetch request.
	 * 
	 * @param handler the handler
	 */
	public void addEndpointHandler(ThrowingFunction<Endpoint, String, OpenemsNamedException> handler) {
		this.urlHandler.add(handler);
	}

	public void setOnTaskFinished(Runnable onTaskFinished) {
		this.onTaskFinished = onTaskFinished == null ? EMPTY_RUNNABLE : onTaskFinished;
	}

	@Override
	public byte[] fetchEndpointRaw(Endpoint endpoint) throws OpenemsNamedException {
	    // Simulated response data
	    byte[] simulatedResponse = "simulated response".getBytes();
	    // Optionally, trigger any task finished actions.
	    this.onTaskFinished.run();
	    return simulatedResponse;
	}

}
