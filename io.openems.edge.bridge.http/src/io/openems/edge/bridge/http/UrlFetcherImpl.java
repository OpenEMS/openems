package io.openems.edge.bridge.http;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.osgi.service.component.annotations.Component;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;

@Component
public class UrlFetcherImpl implements UrlFetcher {

	@Override
	public String fetchEndpoint(final Endpoint endpoint) throws OpenemsNamedException {
		try {
			var url = new URL(endpoint.url());
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

			final var status = con.getResponseCode();
			String body;
			try (var in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				// Read HTTP response
				body = in.lines().collect(joining(System.lineSeparator()));
			}

			// Check valid for all?
			if (status < 300) {
				return body;
			} else {
				throw new OpenemsException(
						"Error while reading Endpoint " + endpoint.url() + ". Response code: " + status + ". " + body);
			}

		} catch (IOException e) {
			throw new OpenemsException(e);
		}
	}

	@Override
	public byte[] fetchEndpointRaw(Endpoint endpoint) throws OpenemsNamedException {
		try {
			URL url = new URL(endpoint.url());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(endpoint.method().name());
			// Set request properties
			endpoint.properties().forEach(connection::setRequestProperty);
			// Handle request body for POST and PUT methods
			if ("POST".equals(endpoint.method().name()) || "PUT".equals(endpoint.method().name())) {
				connection.setDoOutput(true);
				try (OutputStream os = connection.getOutputStream()) {
					byte[] input = endpoint.body().getBytes("utf-8");
					os.write(input, 0, input.length);
				}
			}
			// Read response
			try (InputStream in = connection.getInputStream()) {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				int nRead;
				byte[] data = new byte[1024];
				while ((nRead = in.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}
				buffer.flush();
				return buffer.toByteArray();
			} finally {
				connection.disconnect();
			}
		} catch (IOException e) {
			throw new OpenemsException(e);
		}
	}

}
