package io.openems.backend.metadata.odoo;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.backend.metadata.api.MetadataService;
import io.openems.common.OpenemsException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Designate(ocd = OdooImpl.Config.class, factory = true)
@Component(name = "io.openems.backend.metadata.odoo")
public class OdooImpl implements MetadataService {
	
	@ObjectClassDefinition
	@interface Config {
		String database();
		int uid();
		String password();
		String url() default "https://www1.fenecon.de";
	}

	private String url;
	private String database;
	private int uid;
	private String password;

	@Activate
	void activate(Config config) {
		this.url = config.url();
		this.database = config.database();
		this.uid = config.uid();
		this.password = config.password();
	}

	@Deactivate
	void deactivate() {
	}
	
	//	private MetadataDeviceModel deviceModel;

	//	@Override
	//	public MetadataDeviceModel getDeviceModel() {
	//		return deviceModel;
	//	}

	/**
	 * Tries to authenticate at the Odoo server using a sessionId from a cookie. Updates the Session object accordingly.
	 *
	 * @param sessionId
	 * @return
	 * @throws OpenemsException
	 */
	@Override
	// public void getInfoWithSession(BrowserSession session) throws OpenemsException {
	public void getInfoWithSession() throws OpenemsException {
		HttpURLConnection connection = null;
		try {
			// get session_id from Session
			// SessionData sessionData = session.getData();
			// if (!(sessionData instanceof BrowserSessionData)) {
			// 	throw new OpenemsException("Session is of wrong type.");
			// }
			// BrowserSessionData data = (BrowserSessionData) sessionData;
			// if (!(data.getOdooSessionId().isPresent())) {
			// 	throw new OpenemsException("Session-ID is missing.");
			// }
			// String sessionId = data.getOdooSessionId().get();
			String sessionId = "";

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
				System.out.println(line);
				JsonObject j = (new JsonParser()).parse(line).getAsJsonObject();
//				if (j.has("error")) {
//					JsonObject jError = JsonUtils.getAsJsonObject(j, "error");
//					String errorMessage = JsonUtils.getAsString(jError, "message");
//					throw new OpenemsException("Odoo replied with error: " + errorMessage);
//				}

//				if (j.has("result")) {
//					// parse the result
//					JsonObject jResult = JsonUtils.getAsJsonObject(j, "result");
//					JsonObject jUser = JsonUtils.getAsJsonObject(jResult, "user");
//					data.setUserId(JsonUtils.getAsInt(jUser, "id"));
//					data.setUserName(JsonUtils.getAsString(jUser, "name"));
//					JsonArray jDevices = JsonUtils.getAsJsonArray(jResult, "devices");
//					LinkedHashMultimap<String, DeviceImpl> deviceMap = LinkedHashMultimap.create();
//					for (JsonElement jDevice : jDevices) {
//						String name = JsonUtils.getAsString(jDevice, "name");
//						deviceMap.put(name, new DeviceImpl( //
//								name, //
//								JsonUtils.getAsString(jDevice, "comment"), //
//								JsonUtils.getAsString(jDevice, "producttype"), //
//								JsonUtils.getAsString(jDevice, "role")));
//					}
//					data.setDevices(deviceMap);
//					return;
//				}
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
