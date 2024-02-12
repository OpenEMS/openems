package io.openems.edge.bridge.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.osgi.service.component.annotations.Component;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.HttpMethod;

@Component
public class UrlFetcherImpl implements UrlFetcher {

	@Override
	public String fetchEndpoint(final Endpoint endpoint) throws Exception {
		var url = new URL(endpoint.url());
		var con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod(endpoint.method().name());
		con.setConnectTimeout(endpoint.connectTimeout());
		con.setReadTimeout(endpoint.readTimeout());
		
		endpoint.properties().forEach(con::setRequestProperty);

		if (endpoint.method() == HttpMethod.POST && endpoint.body() != null) {
			con.setDoOutput(true);
			try (var os = con.getOutputStream(); //
					var osw = new OutputStreamWriter(os, "UTF-8")) {
				osw.write(endpoint.body());
				osw.flush();
			}
		}

		var status = con.getResponseCode();
		String body;
		try (var in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			// Read HTTP response
			var content = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				content.append(line);
				content.append(System.lineSeparator());
			}
			body = content.toString();
		}

		// Check valid for all?
		if (status < 300) {
			return body;
		} else {
			throw new OpenemsException(
					"Error while reading Endpoint " + endpoint.url() + ". Response code: " + status + ". " + body);
		}
	}

}
