package io.openems.edge.bridge.http;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;

import org.osgi.service.component.annotations.Component;

import io.openems.common.types.HttpStatus;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.EndpointFetcher;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;

@Component
public class NetworkEndpointFetcher implements EndpointFetcher {

	@Override
	public HttpResponse<String> fetchEndpoint(final Endpoint endpoint) throws HttpError {
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
			return new HttpResponse<>(status, body);
		} catch (IOException e) {
			throw new HttpError.UnknownError(e);
		}
	}

}
