package io.openems.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.browserwebsocket.BrowserWebsocket;
import io.openems.backend.influx.Influxdb;
import io.openems.backend.odoo.Odoo;
import io.openems.backend.openemswebsocket.OpenemsWebsocket;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		log.info("OpenEMS-Backend starting...");

		// Configure everything
		initOdoo();
		initInfluxdb();
		initOpenemsWebsocket();
		initBrowserWebsocket();

		log.info("OpenEMS-Backend started.");
		log.info("");
	}

	private static void initOdoo() throws Exception {
		int port = Integer.valueOf(System.getenv("ODOO_PORT"));
		log.info("Connect to Odoo on port [" + port + "]");
		String url = System.getenv("ODOO_URL");
		String database = System.getenv("ODOO_DATABASE");
		String username = System.getenv("ODOO_USERNAME");
		String password = System.getenv("ODOO_PASSWORD");
		Odoo.initialize(url, port, database, username, password);
	}

	private static void initInfluxdb() throws Exception {
		int port = Integer.valueOf(System.getenv("INFLUX_PORT"));
		log.info("Connect to InfluxDB on port [" + port + "]");
		String database = System.getenv("INFLUX_DATABASE");
		String url = System.getenv("INFLUX_URL");
		String username = System.getenv("INFLUX_USERNAME");
		String password = System.getenv("INFLUX_PASSWORD");
		Influxdb.initialize(database, url, port, username, password);
	}

	private static void initOpenemsWebsocket() throws Exception {
		int port = Integer.valueOf(System.getenv("OPENEMS_WEBSOCKET_PORT"));
		log.info("Start OpenEMS Websocket server on port [" + port + "]");
		OpenemsWebsocket.initialize(port);
	}

	private static void initBrowserWebsocket() throws Exception {
		int port = Integer.valueOf(System.getenv("BROWSER_WEBSOCKET_PORT"));
		log.info("Start Browser Websocket server on port [" + port + "]");
		BrowserWebsocket.initialize(port);
	}
}
