package io.openems.edge.evcs.openwb;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Status;

public class ReadWorker extends AbstractCycleWorker {

	private static final String URL_GET_POWER = "power";
	private static final String URL_GET_POWERS = "powers";
	private static final String URL_GET_VOLTAGES = "voltages";
	private static final String URL_GET_CURRENTS = "currents";
	private static final String URL_GET_CHARGE_STATE = "charge_state";
	private static final String URL_GET_PLUG_STATE = "plug_state";
	private static final String URL_IMPORTED = "imported";

	private final EvcsOpenWbImpl parent;
	private final String baseUrl;

	private Integer energyStartSession = null;

	protected ReadWorker(EvcsOpenWbImpl parent, Inet4Address ip, int port, ChargePoint chargePoint)
			throws NoSuchAlgorithmException, KeyManagementException {
		this.parent = parent;

		this.baseUrl = "https://" + ip.getHostAddress() + ":" + port + "/v1?topic=openWB/internal_chargepoint/"
				+ chargePoint.value + "/get/";
		this.energyStartSession = null;

		parent._setStatus(Status.NOT_READY_FOR_CHARGING);

		/*
		 * Disable SSL certificate checking
		 */
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
		HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
	}

	@Override
	protected void forever() {
		final var openWb = this.parent;

		Integer activePower = null;
		Integer activePowerL1 = null;
		Integer activePowerL2 = null;
		Integer activePowerL3 = null;
		Integer voltageL1 = null;
		Integer voltageL2 = null;
		Integer voltageL3 = null;
		Integer currentL1 = null;
		Integer currentL2 = null;
		Integer currentL3 = null;
		Integer energyTotal = null;
		Integer energySession = null;
		Status status = null;

		var communicationError = false;

		try {
			/**
			 * ElectricityMeter channels.
			 */
			var voltageResponse = this.getResponse(URL_GET_VOLTAGES);
			var voltages = JsonUtils.getAsJsonArray(voltageResponse, "message");
			voltageL1 = JsonUtils.getAsInt(voltages.get(0)) * 1000;
			voltageL2 = JsonUtils.getAsInt(voltages.get(1)) * 1000;
			voltageL3 = JsonUtils.getAsInt(voltages.get(2)) * 1000;

			var powerResponse = this.getResponse(URL_GET_POWERS);
			var powers = JsonUtils.getAsJsonArray(powerResponse, "message");
			activePowerL1 = JsonUtils.getAsInt(powers.get(0));
			activePowerL2 = JsonUtils.getAsInt(powers.get(1));
			activePowerL3 = JsonUtils.getAsInt(powers.get(2));
			activePower = Math.round(JsonUtils.getAsFloat(this.getResponse(URL_GET_POWER), "message"));

			var currentResponse = this.getResponse(URL_GET_CURRENTS);
			var currents = JsonUtils.getAsJsonArray(currentResponse, "message");
			currentL1 = JsonUtils.getAsInt(currents.get(0)) * 1000;
			currentL2 = JsonUtils.getAsInt(currents.get(1)) * 1000;
			currentL3 = JsonUtils.getAsInt(currents.get(2)) * 1000;

			/**
			 * EVCS channels.
			 */
			openWb._setChargingType(ChargingType.AC);

			/*
			 * Read total energy from the box and calculate session energy
			 */
			energyTotal = Math.round(JsonUtils.getAsFloat(this.getResponse(URL_IMPORTED), "message"));
			if (this.energyStartSession != null) {
				energySession = (int) Math.max(0, energyTotal - this.energyStartSession);
			}

			/*
			 * There are only two boolean state values plug state (unplugged and plugged)
			 * and charge state (charging and not charging)
			 */
			var plugState = JsonUtils.getAsBoolean(this.getResponse(URL_GET_PLUG_STATE), "message");
			var chargeState = JsonUtils.getAsBoolean(this.getResponse(URL_GET_CHARGE_STATE), "message");

			if (chargeState) {
				status = Status.CHARGING;

				if (openWb.getStatus() == Status.NOT_READY_FOR_CHARGING) { // Session starts if plugged in
					this.energyStartSession = energyTotal;
				}

			} else if (plugState) {
				status = Status.READY_FOR_CHARGING;

				if (openWb.getStatus() == Status.NOT_READY_FOR_CHARGING) { // Session starts if plugged in
					this.energyStartSession = energyTotal;
				}

			} else {
				status = Status.NOT_READY_FOR_CHARGING;
			}

		} catch (OpenemsNamedException e) {
			communicationError = true;
		}

		openWb._setVoltageL1(voltageL1);
		openWb._setVoltageL2(voltageL2);
		openWb._setVoltageL3(voltageL3);

		openWb._setActivePower(activePower);
		openWb._setActivePowerL1(activePowerL1);
		openWb._setActivePowerL2(activePowerL2);
		openWb._setActivePowerL3(activePowerL3);

		openWb._setCurrentL1(currentL1);
		openWb._setCurrentL2(currentL2);
		openWb._setCurrentL3(currentL3);

		openWb._setStatus(status);

		openWb._setEnergySession(energySession);

		setValue(openWb, EvcsOpenWb.ChannelId.SLAVE_COMMUNICATION_FAILED, communicationError);
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
			var url = URI.create(this.baseUrl + path).toURL();
			var connection = (HttpsURLConnection) url.openConnection();
			connection.setHostnameVerifier((hostname, session) -> true);
			try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				var content = reader.lines().collect(Collectors.joining());
				return JsonUtils.parseToJsonObject(content);
			}
		} catch (IOException e) {
			throw new OpenemsException(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
}
