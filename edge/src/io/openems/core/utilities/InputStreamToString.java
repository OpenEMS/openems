package io.openems.core.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputStreamToString implements Callable<String> {

	private Logger log = LoggerFactory.getLogger(InputStreamToString.class);
	private final InputStream stream;
	private final String logId;

	public InputStreamToString(String logId, InputStream stream) {
		this.logId = logId;
		this.stream = stream;
	}

	@Override
	public String call() {
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = null;
		String line = null;
		try {
			reader = new BufferedReader(new InputStreamReader(stream));
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append("\n");
				log.info("[" + StringUtils.toShortString(logId, 15) + "] " + line);
			}
		} catch (IOException e) {
			builder.append(e.getMessage());
			builder.append("\n");
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					/* ignore */
				}
			}
		}
		return builder.toString();
	}
}