package io.openems.edge.tesla.powerwall2.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractCycleWorker;

public class ReadWorker extends AbstractCycleWorker {

	private static final String URL_SYSTEM_STATUS_SOE = "/system_status/soe";
	private static final String URL_METERS_AGGREGATES = "/meters/aggregates";
	private static final String URL_LOGIN = "/login/Basic";

	private final TeslaPowerwall2CoreImpl parent;
	private final String baseUrl;
	final String emailAddress;
	final String password;

	private final SSLSocketFactory sslSocketFactory;
	private List<String> cookies = new ArrayList<String>();

	protected ReadWorker(TeslaPowerwall2CoreImpl parent, Inet4Address ipAddress, int port, String emailAddress,
			String password) throws NoSuchAlgorithmException, KeyManagementException {
		this.parent = parent;
		this.baseUrl = "https://" + ipAddress.getHostAddress() + ":" + port + "/api";
		this.emailAddress = emailAddress;
		this.password = password;
		/*
		 * Disable SSL certificate checking
		 */
		// only for this worker's connections; setting the default factory
		// of HttpsURLConnection disabled certificate checking for every HTTPS request
		// of the whole Edge (e.g. BridgeHttp: tariffs, cloud APIs with tokens)
		var context = SSLContext.getInstance("TLSv1.2");
		TrustManager[] trustManager = { new X509TrustManager() {
			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certificate, String str) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certificate, String str) {
			}
		} };
		context.init(null, trustManager, new SecureRandom());
		this.sslSocketFactory = context.getSocketFactory();
	}

	@Override
	protected void forever() throws Throwable {
		final var communicationError = new AtomicBoolean(false);

		this.parent.getBattery().ifPresent(battery -> {

			try {
				if (this.cookies.isEmpty()) {
					this.cookies = this.login(URL_LOGIN);
				}
				var soe = this.getResponse(URL_SYSTEM_STATUS_SOE);
				battery._setSoc(Math.round(JsonUtils.getAsFloat(soe, "percentage")));

				var agg = this.getResponse(URL_METERS_AGGREGATES);
				var aggBattery = JsonUtils.getAsJsonObject(agg, "battery");
				var essActivePower = JsonUtils.getAsFloat(aggBattery, "instant_power");
				battery._setActivePower(Math.round(essActivePower));
				var essReactivePower = JsonUtils.getAsFloat(aggBattery, "instant_reactive_power");
				battery._setReactivePower(Math.round(essReactivePower));
				switch (battery.getPhase()) {
				case L1:
					battery._setActivePowerL1(Math.round(essActivePower));
					battery._setActivePowerL2(0);
					battery._setActivePowerL3(0);
					battery._setReactivePowerL1(Math.round(essActivePower));
					battery._setReactivePowerL2(0);
					battery._setReactivePowerL3(0);
					break;
				case L2:
					battery._setActivePowerL1(0);
					battery._setActivePowerL2(Math.round(essActivePower));
					battery._setActivePowerL3(0);
					battery._setReactivePowerL1(0);
					battery._setReactivePowerL2(Math.round(essActivePower));
					battery._setReactivePowerL3(0);
					break;
				case L3:
					battery._setActivePowerL1(0);
					battery._setActivePowerL2(0);
					battery._setActivePowerL3(Math.round(essActivePower));
					battery._setReactivePowerL1(0);
					battery._setReactivePowerL2(0);
					battery._setReactivePowerL3(Math.round(essActivePower));
					break;
				}
				battery._setActiveChargeEnergy(Math.round(JsonUtils.getAsFloat(aggBattery, "energy_imported")));
				battery._setActiveDischargeEnergy(Math.round(JsonUtils.getAsFloat(aggBattery, "energy_exported")));

			} catch (OpenemsNamedException e) {
				communicationError.set(true);
			}

		});

		this.parent._setSlaveCommunicationFailed(communicationError.get());
	}

	// certificate and hostname checks are disabled per connection, not
	// globally
	/**
	 * Opens a HTTPS connection to the Powerwall that accepts its self-signed
	 * certificate.
	 *
	 * @param path the api path
	 * @return the (not yet connected) {@link HttpsURLConnection}
	 * @throws IOException on error
	 */
	HttpsURLConnection openConnection(String path) throws IOException {
		var url = URI.create(this.baseUrl + path).toURL();
		var connection = (HttpsURLConnection) url.openConnection();
		connection.setSSLSocketFactory(this.sslSocketFactory);
		connection.setHostnameVerifier((hostname, session) -> true);
		return connection;
	}

	/**
	 * Gets the JSON response of a HTTPS GET Request.
	 *
	 * @param path the api path
	 * @return the JsonObject
	 * @throws OpenemsNamedException on error
	 */
	private JsonObject getResponse(String path) throws OpenemsNamedException {
		try {
			// connection with this worker's own trust-all socket factory
			var connection = this.openConnection(path);
			for (String cookie : this.cookies) {
				connection.setRequestProperty("Cookie", cookie);
			}

			try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				var content = reader.lines().collect(Collectors.joining());
				return JsonUtils.parseToJsonObject(content);
			}
		} catch (IOException e) {
			throw new OpenemsException(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	/**
	 * Gets the session-cookies of a login HTTPS POST Request.
	 *
	 * @param path the api path
	 * @return session-cookies
	 * @throws OpenemsNamedException on error
	 */
	private List<String> login(String path) throws OpenemsNamedException {
		try {
			var connection = this.openConnection(path);
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setDoOutput(true);
			String postParams = "username=customer&email=" + this.emailAddress + "&password=" + this.password
					+ "&force_sm_off=false";
			OutputStream os = connection.getOutputStream();
			os.write(postParams.getBytes());
			os.flush();
			os.close();

			var code = connection.getResponseCode();
			if (code != HttpURLConnection.HTTP_OK) {
				this.cookies.clear();
				throw new OpenemsException("login failed: got HTTP-code:" + code);
			}
			List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
			return cookies;

		} catch (IOException e) {
			throw new OpenemsException(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
}
