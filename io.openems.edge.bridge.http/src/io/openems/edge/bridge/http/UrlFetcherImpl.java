package io.openems.edge.bridge.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Component;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;

@Component
public class UrlFetcherImpl implements UrlFetcher {

	public Runnable createTask(//
			final String urlString, //
			final int connectTimeout, //
			final int readTimeout, //
			final CompletableFuture<String> future //
	) {
		return () -> {
			try {
				var url = new URL(urlString);
				var con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");

				// config setting / method param ?
				con.setConnectTimeout(connectTimeout);
				con.setReadTimeout(readTimeout);

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
					future.complete(body);
				} else {
					throw new OpenemsException(
							"Error while reading from Shelly API. Response code: " + status + ". " + body);
				}
			} catch (OpenemsNamedException | IOException e) {
				future.completeExceptionally(e);
			}
		};
	}

}
