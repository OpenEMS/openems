package io.openems.femsserver.browserwebsocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.femsserver.femswebsocket.Device;
import io.openems.femsserver.utilities.JsonUtils;
import io.openems.femsserver.utilities.OpenemsException;

public class BrowserConnection {

	private final String sessionId;
	
	private final ConcurrentHashMap<String, Device> devices = new ConcurrentHashMap<>();

	public BrowserConnection(String sessionId) throws OpenemsException {
		this.sessionId = sessionId;
		refreshInfo(sessionId);
		System.out.println("Session is valid");
	}

	private void refreshInfo(String sessionId) throws OpenemsException {
		try {
			String charset = "UTF-8";
			String query = String.format("session_id=%s", URLEncoder.encode(sessionId, charset));
			HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:8070/fems/info?" + query)
					.openConnection();
			try {
				connection.setConnectTimeout(5000);// 5 secs
				connection.setReadTimeout(5000);// 5 secs
				connection.setRequestProperty("Accept-Charset", charset);
				connection.setRequestMethod("POST");
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", "application/json");

				OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
				out.write("{}");
				out.flush();
				out.close();
				
				InputStream is = connection.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				String line = null;
				while ((line = br.readLine()) != null) {
					JsonObject j = (new JsonParser()).parse(line).getAsJsonObject();
					if (j.has("result")) {
						// found info
						JsonObject jResult = JsonUtils.getAsJsonObject(j, "result");
						JsonObject jDevices = JsonUtils.getAsJsonObject(jResult, "devices");
						HashSet<String> newDeviceNames = new HashSet<String>(); // store newly added devices
						for(Entry<String, JsonElement> entry : jDevices.entrySet()) {
							/*
							 * fill devices list
							 */
							String name = entry.getKey();
							JsonObject jDevice = entry.getValue().getAsJsonObject();
							String comment = JsonUtils.getAsString(jDevice, "comment");
							Device device = new Device(name, comment);
							this.devices.putIfAbsent(name, device);
							newDeviceNames.add(name);
							System.out.println(device.toString());
						}
						// TODO remove disappeared devices using "newDeviceNames"
					} else {
						// error
						if (j.has("error")) {
							JsonObject jError = JsonUtils.getAsJsonObject(j, "error");
							String errorMessage = JsonUtils.getAsString(jError, "message");
							throw new OpenemsException(errorMessage);
						}
					}
				}
			} finally {
				connection.disconnect();
			}
		} catch (IOException e) {
			throw new OpenemsException(e.getMessage());
		}
	}
}
