package io.openems.backend.odoo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abercap.odoo.Session;

import io.openems.backend.odoo.device.DeviceCache;
import io.openems.backend.odoo.device.DeviceModel;

public class Odoo {

	private final Logger log = LoggerFactory.getLogger(Odoo.class);

	private Session session;
	private DeviceModel deviceModel;
	private DeviceCache deviceCache;

	protected Odoo(String url, int port, String database, String username, String password) throws Exception {
		this.session = new Session(url, port, database, username, password);
		this.connect();
		this.deviceModel = new DeviceModel(this.session);
		this.deviceCache = new DeviceCache();
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
	 * Tries to authenticate at the Odoo server using a sessionId from a cookie. On success, returns a JsonObject with
	 * fems device information.
	 *
	 * @param sessionId
	 * @return
	 * @throws OpenemsException
	 */
	// public JsonObject getFemsInfo(String sessionId) throws OpenemsException {
	// try {
	// String charset = "US-ASCII";
	// String query = String.format("session_id=%s", URLEncoder.encode(sessionId, charset));
	// HttpURLConnection connection = (HttpURLConnection) new URL(
	// "http://" + this.url + ":" + this.port + "/fems/info?" + query).openConnection();
	// try {
	// connection.setConnectTimeout(5000);// 5 secs
	// connection.setReadTimeout(5000);// 5 secs
	// connection.setRequestProperty("Accept-Charset", charset);
	// connection.setRequestMethod("POST");
	// connection.setDoOutput(true);
	// connection.setRequestProperty("Content-Type", "application/json");
	//
	// OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
	// out.write("{}");
	// out.flush();
	// out.close();
	//
	// InputStream is = connection.getInputStream();
	// BufferedReader br = new BufferedReader(new InputStreamReader(is));
	// String line = null;
	// while ((line = br.readLine()) != null) {
	// // parse the result
	// JsonObject j = (new JsonParser()).parse(line).getAsJsonObject();
	// if (j.has("error")) {
	// JsonObject jError = JsonUtils.getAsJsonObject(j, "error");
	// String errorMessage = JsonUtils.getAsString(jError, "message");
	// throw new OpenemsException(errorMessage);
	// }
	// if (j.has("result")) {
	// return JsonUtils.getAsJsonObject(j, "result");
	// }
	// }
	// } finally {
	// connection.disconnect();
	// }
	// throw new OpenemsException("No result from Odoo");
	// } catch (IOException e) {
	// throw new OpenemsException(e.getMessage());
	// }
	// }
}
