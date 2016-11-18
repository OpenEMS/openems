package io.openems.femsserver.odoo;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abercap.odoo.FilterCollection;
import com.abercap.odoo.ObjectAdapter;
import com.abercap.odoo.Row;
import com.abercap.odoo.RowCollection;
import com.abercap.odoo.Session;

public class Odoo {

	// private final String url = "fenecon.de";
	protected String url;
	protected int port;
	protected String database;
	protected String username;
	protected String password;

	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Odoo.class);

	private static Odoo instance;

	public static synchronized void initialize(String url, int port, String database, String username, String password) throws Exception {
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
	}
	
	public static synchronized Odoo getInstance() {
		if (Odoo.instance == null) {
			Odoo.instance = new Odoo();
		}
		return Odoo.instance;
	}

	private Odoo() {}

	private Session getSession() throws Exception {
		Session session = new Session(url, port, database, username, password);
		// startSession logs into the server and keeps the userid of the logged
		// in user
		session.startSession();
		return session;
	}

	public String getDeviceForApikey(String apikey) throws Exception {
		Session session = getSession();
		ObjectAdapter deviceAd = session.getObjectAdapter("fems.device");
		FilterCollection filters = new FilterCollection();
		filters.add("apikey", "=", apikey);
		RowCollection devices = deviceAd.searchAndReadObject(filters, new String[] { "name_number" });
		for (Row device : devices) {
			return device.get("name_number").toString();
		}
		throw new Exception("Device not found.");
	}
}
