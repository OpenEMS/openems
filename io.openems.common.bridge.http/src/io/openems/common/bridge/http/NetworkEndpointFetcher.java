package io.openems.common.bridge.http;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpEventRaiser;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.EndpointFetcherEvents;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.types.DebugMode;
import io.openems.common.types.HttpStatus;

@Component
public class NetworkEndpointFetcher implements EndpointFetcher {

	private final Logger log = LoggerFactory.getLogger(NetworkEndpointFetcher.class);
	private final AtomicLong idCounter = new AtomicLong();

	@Override
	public HttpResponse<String> fetchEndpoint(//
			final BridgeHttp.Endpoint endpoint, //
			final DebugMode mode, //
			final BridgeHttpEventRaiser eventRaiser //
	) throws HttpError {
		final var requestId = this.idCounter.incrementAndGet();
		eventRaiser.raiseEvent(EndpointFetcherEvents.REQUEST_START,
				new EndpointFetcherEvents.RequestStartEvent(requestId, endpoint));
		try {
			final var result = this.fetchEndpointInternal(endpoint, mode);

			eventRaiser.raiseEvent(EndpointFetcherEvents.REQUEST_SUCCESS,
					new EndpointFetcherEvents.RequestSuccessEvent(requestId, result, endpoint));

			return result;
		} catch (Exception e) {
			eventRaiser.raiseEvent(EndpointFetcherEvents.REQUEST_FAILED,
					new EndpointFetcherEvents.RequestFailedEvent(requestId, e, endpoint));
			throw e;
		} finally {
			eventRaiser.raiseEvent(EndpointFetcherEvents.REQUEST_FINISHED,
					new EndpointFetcherEvents.RequestFinishedEvent(requestId, endpoint));
		}
	}

	private HttpResponse<String> fetchEndpointInternal(//
			final BridgeHttp.Endpoint endpoint, //
			final DebugMode mode //
	) throws HttpError {
		try {
			if (mode == DebugMode.DETAILED) {
				this.log.info("Fetching Endpoint for request endpoint: {}", endpoint);
			}

			var url = URI.create(endpoint.url()).toURL();
			var con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod(endpoint.method().name());
			con.setConnectTimeout(endpoint.connectTimeout());
			con.setReadTimeout(endpoint.readTimeout());

			endpoint.properties().forEach(con::setRequestProperty);

			if (endpoint.method().isBodyAllowed() && endpoint.body() != null) {
				con.setDoOutput(true);
				try (var os = con.getOutputStream(); //
						var osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
					osw.write(endpoint.body());
					osw.flush();
				}
			}

			final var status = HttpStatus.fromCodeOrCustom(con.getResponseCode(), con.getResponseMessage());
			if (mode.equals(DebugMode.DETAILED)) {
				this.log.info("Headers: {}", con.getHeaderFields());
			}

			String body;
			try (var in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				// Read HTTP response
				body = in.lines().collect(joining(System.lineSeparator()));
			} catch (IOException e) {
				throw new HttpError.ResponseError(status, null);
			}

			if (status.isError()) {
				throw new HttpError.ResponseError(status, body);
			}
			if (mode.equals(DebugMode.DETAILED)) {
				this.log.info("Fetched Endpoint for request: " + "\n" //
						+ "endpoint: " + endpoint + "\n" //
						+ "result: " + body //
				);
			}

			return new HttpResponse<>(status, con.getHeaderFields(), body);
		} catch (IOException e) {
			throw new HttpError.UnknownError(e);
		}
	}

}
