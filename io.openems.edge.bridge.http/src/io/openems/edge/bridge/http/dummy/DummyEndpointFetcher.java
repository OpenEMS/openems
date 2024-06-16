package io.openems.edge.bridge.http.dummy;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.function.ThrowingFunction;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.EndpointFetcher;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;

public class DummyEndpointFetcher implements EndpointFetcher {

	public record DummyHandler(//
			boolean singleUse, //
			ThrowingFunction<Endpoint, HttpResponse<String>, HttpError> handler //
	) {

	}

	private final Logger log = LoggerFactory.getLogger(DummyEndpointFetcher.class);

	private final List<DummyHandler> urlHandler = new LinkedList<>();
	private Runnable onTaskFinished = FunctionUtils::doNothing;

	@Override
	public HttpResponse<String> fetchEndpoint(//
			final Endpoint endpoint //
	) throws HttpError {
		try {
			for (final var iterator = this.urlHandler.iterator(); iterator.hasNext();) {
				final var dummyHandler = iterator.next();
				try {
					final var result = dummyHandler.handler().apply(endpoint);
					if (result != null) {
						if (dummyHandler.singleUse()) {
							iterator.remove();
						}
						if (result.status().isError()) {
							this.log.info(
									"Throw error response directly instead of returning a error status. Result Status: "
											+ result.status());
							throw new HttpError.ResponseError(result.status(), result.data());
						}
						return result;
					}
				} catch (Exception e) {
					if (dummyHandler.singleUse()) {
						iterator.remove();
					}
					throw e;
				}
			}
			throw HttpError.ResponseError.notFound();
		} catch (RuntimeException e) {
			throw new HttpError.UnknownError(e);
		} finally {
			this.onTaskFinished.run();
		}
	}

	/**
	 * Adds a static handler for a fetch request.
	 * 
	 * @param handler the handler
	 */
	public void addEndpointHandler(ThrowingFunction<Endpoint, HttpResponse<String>, HttpError> handler) {
		this.urlHandler.add(new DummyHandler(false, handler));
	}

	/**
	 * Adds a single use static handler for a fetch request. The handler will be
	 * removed once it produced a valid result.
	 * 
	 * <p>
	 * A valid result would be to throw an {@link Exception} or return a
	 * {@link HttpResponse}. Invalid would be returning <code>null</code>.
	 * 
	 * @param handler the handler to add
	 */
	public void addSingleUseEndpointHandler(ThrowingFunction<Endpoint, HttpResponse<String>, HttpError> handler) {
		this.urlHandler.add(new DummyHandler(true, handler));
	}

	public void setOnTaskFinished(Runnable onTaskFinished) {
		this.onTaskFinished = onTaskFinished == null ? FunctionUtils::doNothing : onTaskFinished;
	}

}
