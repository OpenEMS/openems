package io.openems.edge.tesla.powerwall2.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Inet4Address;

import javax.net.ssl.HttpsURLConnection;

import org.junit.jupiter.api.Test;

public class ReadWorkerTest {

	private static ReadWorker createWorker() throws Exception {
		return new ReadWorker(new TeslaPowerwall2CoreImpl(), (Inet4Address) Inet4Address.getByName("127.0.0.1"),
				443, "test@example.com", "password123");
	}

	@Test
	public void testDefaultSslSocketFactoryIsNotChanged() throws Exception {
		var defaultFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
		var defaultVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

		createWorker();

		assertSame(defaultFactory, HttpsURLConnection.getDefaultSSLSocketFactory());
		assertSame(defaultVerifier, HttpsURLConnection.getDefaultHostnameVerifier());
	}

	@Test
	public void testOpenConnectionUsesOwnSslSocketFactory() throws Exception {
		var worker = createWorker();

		var connection = worker.openConnection("/system_status/soe");

		assertEquals("https://127.0.0.1:443/api/system_status/soe", connection.getURL().toString());
		assertNotSame(HttpsURLConnection.getDefaultSSLSocketFactory(), connection.getSSLSocketFactory());
		assertSame(connection.getSSLSocketFactory(), worker.openConnection("/meters/aggregates").getSSLSocketFactory());
		assertTrue(connection.getHostnameVerifier().verify("powerwall", null));
	}
}
