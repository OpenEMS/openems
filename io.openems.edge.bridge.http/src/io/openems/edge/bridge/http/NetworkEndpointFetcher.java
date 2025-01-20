package io.openems.edge.bridge.http;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.DebugMode;
import io.openems.common.types.HttpStatus;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.EndpointFetcher;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyEndpointFetcher;

@Component
public class NetworkEndpointFetcher implements EndpointFetcher {

	private final Logger log = LoggerFactory.getLogger(DummyEndpointFetcher.class);

	@Override
	public HttpResponse<String> fetchEndpoint(final Endpoint endpoint, DebugMode mode) throws HttpError {
		try {
			var url = URI.create(endpoint.url()).toURL();
			var con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod(endpoint.method().name());
			con.setConnectTimeout(endpoint.connectTimeout());
			con.setReadTimeout(endpoint.readTimeout());

			endpoint.properties().forEach(con::setRequestProperty);

			if (endpoint.method().isBodyAllowed() && endpoint.body() != null) {
				con.setDoOutput(true);
				try (var os = con.getOutputStream(); //
						var osw = new OutputStreamWriter(os, "UTF-8")) {
					osw.write(endpoint.body());
					osw.flush();
				}
			}

			final var status = HttpStatus.fromCodeOrCustom(con.getResponseCode(), con.getResponseMessage());

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
				this.log.debug("Fetched Endpoint for request: " + endpoint.url() + "\n" //
						+ "method: " + endpoint.method().name() + "\n" //
						+ "result: " + body //
				);
			}
			return new HttpResponse<>(status, body);
		} catch (IOException e) {
			throw new HttpError.UnknownError(e);
		}
	}

}
