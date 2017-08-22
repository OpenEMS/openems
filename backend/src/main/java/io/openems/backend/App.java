package io.openems.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.browserwebsocket.BrowserWebsocketProvider;
import io.openems.backend.influx.InfluxdbProvider;
import io.openems.backend.odoo.OdooProvider;
import io.openems.backend.openemswebsocket.OpenemsWebsocketProvider;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		log.info("OpenEMS-Backend starting...");

		// Configure everything
		initOdoo();
		initInfluxdb();
		initOpenemsWebsocket();
		// initBrowserWebsocket();

		log.info("OpenEMS-Backend started.");
	}

	private static void initOdoo() throws Exception {
		int port = Integer.valueOf(System.getenv("ODOO_PORT"));
		log.info("Connect to Odoo on port [" + port + "]");
		String url = System.getenv("ODOO_URL");
		String database = System.getenv("ODOO_DATABASE");
		String username = System.getenv("ODOO_USERNAME");
		String password = System.getenv("ODOO_PASSWORD");
		OdooProvider.initialize(url, port, database, username, password);
	}

	private static void initInfluxdb() throws Exception {
		int port = Integer.valueOf(System.getenv("INFLUX_PORT"));
		log.info("Connect to InfluxDB on port [" + port + "]");
		String database = System.getenv("INFLUX_DATABASE");
		String url = System.getenv("INFLUX_URL");
		String username = System.getenv("INFLUX_USERNAME");
		String password = System.getenv("INFLUX_PASSWORD");
		InfluxdbProvider.initialize(database, url, port, username, password);
	}

	private static void initOpenemsWebsocket() throws Exception {
		int port = Integer.valueOf(System.getenv("OPENEMS_WEBSOCKET_PORT"));
		log.info("Start OpenEMS Websocket server on port [" + port + "]");
		OpenemsWebsocketProvider.initialize(port);
	}

	private static void initBrowserWebsocket() throws Exception {
		int port = Integer.valueOf(System.getenv("BROWSER_WEBSOCKET_PORT"));
		log.info("Start Browser Websocket server on port [" + port + "]");
		BrowserWebsocketProvider.initialize(port);
	}
}
