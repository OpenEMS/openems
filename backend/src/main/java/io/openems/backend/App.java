package io.openems.backend;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.browserwebsocket.BrowserWebsocket;
import io.openems.backend.influx.Influxdb;
import io.openems.backend.metadata.Metadata;
import io.openems.backend.openemswebsocket.OpenemsWebsocket;
import io.openems.common.utils.EnvUtils;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		log.info("OpenEMS-Backend starting...");

		// Configure everything
		initMetadataProvider();
		initInfluxdb();
		initOpenemsWebsocket();
		initBrowserWebsocket();

		log.info("OpenEMS-Backend started.");
		log.info("");
	}

	/**
	 * Configures a metadata provider. It uses either Odoo as backend or a simple Dummy provider.
	 *
	 * @throws Exception
	 */
	private static void initMetadataProvider() throws Exception {
		Optional<String> metadataOpt = EnvUtils.getAsOptionalString("METADATA");
		if (metadataOpt.isPresent() && metadataOpt.get().equals("DUMMY")) {
			Metadata.initializeDummy();
		} else {
			int port = EnvUtils.getAsInt("ODOO_PORT");
			String url = EnvUtils.getAsString("ODOO_URL");
			String database = EnvUtils.getAsString("ODOO_DATABASE");
			log.info("Connect to Odoo. Url [" + url + ":" + port + "] Database [" + database + "]");
			String username = EnvUtils.getAsString("ODOO_USERNAME");
			String password = EnvUtils.getAsString("ODOO_PASSWORD");
			Metadata.initializeOdoo(url, port, database, username, password);
		}
	}

	private static void initInfluxdb() throws Exception {
		int port = Integer.valueOf(System.getenv("INFLUX_PORT"));
		String url = EnvUtils.getAsString("INFLUX_URL");
		String database = EnvUtils.getAsString("INFLUX_DATABASE");
		log.info("Connect to InfluxDB. Url [" + url + ":" + port + "], Database [" + database + "]");
		String username = EnvUtils.getAsString("INFLUX_USERNAME");
		String password = EnvUtils.getAsString("INFLUX_PASSWORD");
		Influxdb.initialize(database, url, port, username, password);
	}

	private static void initOpenemsWebsocket() throws Exception {
		int port = EnvUtils.getAsInt("OPENEMS_WEBSOCKET_PORT");
		log.info("Start OpenEMS Websocket server on port [" + port + "]");
		OpenemsWebsocket.initialize(port);
	}

	private static void initBrowserWebsocket() throws Exception {
		int port = EnvUtils.getAsInt("BROWSER_WEBSOCKET_PORT");
		log.info("Start Browser Websocket server on port [" + port + "]");
		BrowserWebsocket.initialize(port);
	}
}
