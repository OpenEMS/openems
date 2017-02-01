package io.openems.femsserver.odoo;

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

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abercap.odoo.OdooApiException;
import com.abercap.odoo.Session;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.femsserver.odoo.fems.device.FemsDevice;
import io.openems.femsserver.odoo.fems.device.FemsDeviceModel;
import io.openems.femsserver.utilities.JsonUtils;
import io.openems.femsserver.utilities.OpenemsException;

public class Odoo {

	private static Logger log = LoggerFactory.getLogger(Odoo.class);

	private static Odoo instance;

	public static synchronized void initialize(String url, int port, String database, String username, String password)
			throws Exception {
		if (url == null || database == null || username == null || password == null) {
			throw new Exception("Config missing: database [" + database + "], url [" + url + "], port [" + port
					+ "] username [" + username + "], password [" + password + "]");
		}
		Odoo odoo = getInstance();
		odoo.url = url;
		odoo.port = port;
		odoo.database = database;
		odoo.username = username;
		odoo.password = password;

		odoo.connect();
	}

	public static synchronized Odoo getInstance() {
		if (Odoo.instance == null) {
			Odoo.instance = new Odoo();
		}
		return Odoo.instance;
	}

	private String url;
	private int port;
	private String database;
	private String username;
	private String password;
	private Session session;
	private FemsDeviceModel femsDeviceModel;

	private Odoo() {}

	private void connect() throws Exception {
		session = new Session(url, port, database, username, password);
		// startSession logs into the server and keeps the userid of the logged
		// in user
		session.startSession();
		femsDeviceModel = new FemsDeviceModel(session);
	}

	public List<FemsDevice> getDevicesForApikey(String apikey) throws OdooApiException, XmlRpcException {
		List<FemsDevice> devices = femsDeviceModel.searchAndReadObject("apikey", "=", apikey);
		return devices;
	}

	public List<FemsDevice> getDevicesForName(String name) throws OdooApiException, XmlRpcException {
		List<FemsDevice> devices = femsDeviceModel.searchAndReadObject("name", "=", name);
		return devices;
	}

	public List<FemsDevice> getDevicesForNames(List<String> names) throws OdooApiException, XmlRpcException {
		// TODO optimize: use only one call to searchAndReadObject
		List<FemsDevice> devices = new ArrayList<>();
		for (String name : names) {
			devices.addAll(femsDeviceModel.searchAndReadObject("name", "=", name));
		}
		return devices;
	}

	public List<FemsDevice> getDevicesForNames(JsonArray jNames) throws OdooApiException, XmlRpcException {
		// TODO optimize: use only one call to searchAndReadObject
		List<FemsDevice> devices = new ArrayList<>();
		jNames.forEach(jName -> {
			String name = jName.getAsString();
			try {
				devices.addAll(femsDeviceModel.searchAndReadObject("name", "=", name));
			} catch (XmlRpcException | OdooApiException e) {
				log.error(e.getMessage());
			}
		});
		return devices;
	}

	/**
	 * Tries to authenticate at the Odoo server using a sessionId from a cookie. On success, returns a JsonObject with
	 * fems device information.
	 *
	 * @param sessionId
	 * @return
	 * @throws OpenemsException
	 */
	public JsonObject getFemsInfo(String sessionId) throws OpenemsException {
		try {
			String charset = "US-ASCII";
			String query = String.format("session_id=%s", URLEncoder.encode(sessionId, charset));
			HttpURLConnection connection = (HttpURLConnection) new URL(
					"http://" + this.url + ":" + this.port + "/fems/info?" + query).openConnection();
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
					// parse the result
					JsonObject j = (new JsonParser()).parse(line).getAsJsonObject();
					if (j.has("error")) {
						JsonObject jError = JsonUtils.getAsJsonObject(j, "error");
						String errorMessage = JsonUtils.getAsString(jError, "message");
						throw new OpenemsException(errorMessage);
					}
					if (j.has("result")) {
						return JsonUtils.getAsJsonObject(j, "result");
					}
				}
			} finally {
				connection.disconnect();
			}
			throw new OpenemsException("No result from Odoo");
		} catch (IOException e) {
			throw new OpenemsException(e.getMessage());
		}
	}
}
