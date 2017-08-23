package io.openems.backend.odoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abercap.odoo.Session;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.browserwebsocket.DeviceInfo;
import io.openems.backend.browserwebsocket.session.BrowserSession;
import io.openems.backend.browserwebsocket.session.BrowserSessionData;
import io.openems.backend.odoo.device.DeviceCache;
import io.openems.backend.odoo.device.DeviceModel;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.SessionData;
import io.openems.common.utils.JsonUtils;

public class OdooSingleton {
	private final Logger log = LoggerFactory.getLogger(OdooSingleton.class);

	private Session session;
	private DeviceModel deviceModel;
	private DeviceCache deviceCache;
	private final String url;

	protected OdooSingleton(String url, int port, String database, String username, String password) throws Exception {
		this.session = new Session(url, port, database, username, password);
		this.connect();
		this.deviceModel = new DeviceModel(this.session);
		this.deviceCache = new DeviceCache();
		this.url = "http://" + url + ":" + port;
	}

	private void connect() throws Exception {
		session.startSession();
	}

	public DeviceModel getDeviceModel() {
		return deviceModel;
	}

	public DeviceCache getDeviceCache() {
		return deviceCache;
	}

	// public List<Device> getDevicesForApikey(String apikey) throws OdooApiException, XmlRpcException {
	// List<Device> devices = deviceModel.searchAndReadObject("apikey", "=", apikey);
	// return devices;
	// }
	//
	// public List<Device> getDevicesForName(String name) throws OdooApiException, XmlRpcException {
	// List<Device> devices = deviceModel.searchAndReadObject("name", "=", name);
	// return devices;
	// }
	//
	// public List<Device> getDevicesForNames(List<String> names) throws OdooApiException, XmlRpcException {
	// // TODO optimize: use only one call to searchAndReadObject
	// List<Device> devices = new ArrayList<>();
	// for (String name : names) {
	// devices.addAll(deviceModel.searchAndReadObject("name", "=", name));
	// }
	// return devices;
	// }

	/**
	 *
	 * @param jNames
	 *            [{ name: 'fems1', role: 'guest' }]
	 * @return
	 * @throws OdooApiException
	 * @throws XmlRpcException
	 * @throws OpenemsException
	 */
	// public List<Device> getDevicesForNames(JsonArray jDevices)
	// throws OdooApiException, XmlRpcException, OpenemsException {
	// // TODO optimize: use only one call to searchAndReadObject
	// List<Device> devices = new ArrayList<>();
	// for (JsonElement jDeviceElement : jDevices) {
	// JsonObject jDevice = JsonUtils.getAsJsonObject(jDeviceElement);
	// String name = JsonUtils.getAsString(jDevice, "name");
	// String role = JsonUtils.getAsString(jDevice, "role");
	// try {
	// List<Device> nameDevices = deviceModel.searchAndReadObject("name", "=", name);
	// nameDevices.forEach(device -> {
	// device.setRole(role);
	// });
	// devices.addAll(nameDevices);
	// } catch (XmlRpcException | OdooApiException e) {
	// log.error(e.getMessage());
	// }
	// }
	// return devices;
	// }

	/**
	 * Tries to authenticate at the Odoo server using a sessionId from a cookie. Updates the Session object accordingly.
	 *
	 * @param sessionId
	 * @return
	 * @throws OpenemsException
	 */
	public void getInfoWithSession(BrowserSession session) {
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
			HttpURLConnection connection = (HttpURLConnection) new URL(this.url + "/openems_backend/info?" + query)
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
					if (j.has("error")) {
						JsonObject jError = JsonUtils.getAsJsonObject(j, "error");
						String errorMessage = JsonUtils.getAsString(jError, "message");
						throw new OpenemsException(errorMessage);
					}

					if (j.has("result")) {
						// parse the result
						JsonObject jResult = JsonUtils.getAsJsonObject(j, "result");
						data.setUserId(JsonUtils.getAsInt(jResult, "user"));
						JsonArray jDevices = JsonUtils.getAsJsonArray(jResult, "devices");
						List<DeviceInfo> deviceInfos = new ArrayList<>();
						for (JsonElement jDevice : jDevices) {
							deviceInfos.add(new DeviceInfo(JsonUtils.getAsString(jDevice, "name"),
									JsonUtils.getAsString(jDevice, "role")));
						}
						data.setDeviceInfos(deviceInfos);
						session.setValid();
						return;
					}
				}
			} finally {
				connection.disconnect();
			}
			throw new OpenemsException("No result from Odoo");
		} catch (IOException | OpenemsException e) {
			log.warn(e.getMessage());
			session.setInvalid();
		}
	}
}
