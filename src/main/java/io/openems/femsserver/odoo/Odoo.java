package io.openems.femsserver.odoo;

import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abercap.odoo.OdooApiException;
import com.abercap.odoo.Session;

import io.openems.femsserver.odoo.fems.device.FemsDevice;
import io.openems.femsserver.odoo.fems.device.FemsDeviceModel;

public class Odoo {

	@SuppressWarnings("unused")
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

	private Odoo() {
	}

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
	
	public FemsDevice getFirstDeviceForApikey(String apikey) throws OdooApiException, XmlRpcException {
		List<FemsDevice> devices = getDevicesForApikey(apikey);
		if(devices.size() > 0) {
			return devices.get(0);
		} else {
			return null;
		}
	}
}
