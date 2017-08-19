package io.openems.femsserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.femsserver.browserwebsocket.BrowserWebsocket;
import io.openems.femsserver.femswebsocket.FemsWebsocket;
import io.openems.femsserver.influx.Influxdb;
import io.openems.femsserver.odoo.Odoo;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		log.info("FEMS-Server starting...");

		// Configure everything
		initOdoo();
		initInfluxdb();
		initFemsWebsocket();
		initBrowserWebsocket();

		log.info("FEMS-Server started.");
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

	private static void initFemsWebsocket() throws Exception {
		int port = Integer.valueOf(System.getenv("OPENEMS_WEBSOCKET_PORT"));
		log.info("Start FEMS Websocket server on port [" + port + "]");
		FemsWebsocket.initialize(port);
	}

	private static void initBrowserWebsocket() throws Exception {
		int port = Integer.valueOf(System.getenv("BROWSER_WEBSOCKET_PORT"));
		log.info("Start Browser Websocket server on port [" + port + "]");
		BrowserWebsocket.initialize(port);
	}
}
