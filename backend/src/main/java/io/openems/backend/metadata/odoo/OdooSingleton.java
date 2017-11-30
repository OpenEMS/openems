package io.openems.backend.metadata.odoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.xmlrpc.XmlRpcException;

import com.google.common.collect.LinkedHashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.odoojava.api.OdooApiException;
import com.odoojava.api.Session;

import io.openems.backend.browserwebsocket.session.BrowserSession;
import io.openems.backend.browserwebsocket.session.BrowserSessionData;
import io.openems.backend.metadata.api.MetadataSingleton;
import io.openems.backend.metadata.api.device.MetadataDeviceModel;
import io.openems.backend.metadata.odoo.device.OdooDeviceModel;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.SessionData;
import io.openems.common.types.DeviceImpl;
import io.openems.common.utils.JsonUtils;

public class OdooSingleton implements MetadataSingleton {
	private Session session;
	private MetadataDeviceModel deviceModel;
	private final String url;

	public OdooSingleton(String url, int port, String database, String username, String password)
			throws OpenemsException {
		this.session = new Session(url, port, database, username, password);
		this.connect();
		try {
			this.deviceModel = new OdooDeviceModel(this.session);
		} catch (XmlRpcException | OdooApiException e) {
			throw new OpenemsException("Initializing OdooDeviceModel failed: " + e.getMessage());
		}
		this.url = "http://" + url + ":" + port;
	}

	private void connect() throws OpenemsException {
		try {
			session.startSession();
		} catch (Exception e) {
			throw new OpenemsException("Odoo connection failed: " + e.getMessage());
		}
	}

	@Override
	public MetadataDeviceModel getDeviceModel() {
		return deviceModel;
	}

	/**
	 * Tries to authenticate at the Odoo server using a sessionId from a cookie. Updates the Session object accordingly.
	 *
	 * @param sessionId
	 * @return
	 * @throws OpenemsException
	 */
	@Override
	public void getInfoWithSession(BrowserSession session) throws OpenemsException {
		HttpURLConnection connection = null;
		try {
			// get session_id from Session
			SessionData sessionData = session.getData();
			if (!(sessionData instanceof BrowserSessionData)) {
				throw new OpenemsException("Session is of wrong type.");
			}
			BrowserSessionData data = (BrowserSessionData) sessionData;
			if (!(data.getOdooSessionId().isPresent())) {
				throw new OpenemsException("Session-ID is missing.");
			}
			String sessionId = data.getOdooSessionId().get();

			// send request to Odoo
			String charset = "US-ASCII";
			String query = String.format("session_id=%s", URLEncoder.encode(sessionId, charset));
			connection = (HttpURLConnection) new URL(this.url + "/openems_backend/info?" + query).openConnection();
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
				if (j.has("error")) {
					JsonObject jError = JsonUtils.getAsJsonObject(j, "error");
					String errorMessage = JsonUtils.getAsString(jError, "message");
					throw new OpenemsException("Odoo replied with error: " + errorMessage);
				}

				if (j.has("result")) {
					// parse the result
					JsonObject jResult = JsonUtils.getAsJsonObject(j, "result");
					JsonObject jUser = JsonUtils.getAsJsonObject(jResult, "user");
					data.setUserId(JsonUtils.getAsInt(jUser, "id"));
					data.setUserName(JsonUtils.getAsString(jUser, "name"));
					JsonArray jDevices = JsonUtils.getAsJsonArray(jResult, "devices");
					LinkedHashMultimap<String, DeviceImpl> deviceMap = LinkedHashMultimap.create();
					for (JsonElement jDevice : jDevices) {
						String name = JsonUtils.getAsString(jDevice, "name");
						deviceMap.put(name, new DeviceImpl( //
								name, //
								JsonUtils.getAsString(jDevice, "comment"), //
								JsonUtils.getAsString(jDevice, "producttype"), //
								JsonUtils.getAsString(jDevice, "role")));
					}
					data.setDevices(deviceMap);
					return;
				}
			}
		} catch (IOException e) {
			throw new OpenemsException("IOException while reading from Odoo: " + e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		throw new OpenemsException("No result from Odoo");
	}
}
