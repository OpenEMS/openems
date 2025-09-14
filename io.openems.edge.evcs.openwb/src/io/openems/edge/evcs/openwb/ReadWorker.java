package io.openems.edge.evcs.openwb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;
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

import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;


public class ReadWorker extends AbstractCycleWorker {

	private static final String URL_GET_POWER = "power";
	private static final String URL_GET_POWERS = "powers";
	private static final String URL_GET_VOLTAGES = "voltages";
	private static final String URL_GET_CURRENTS = "currents";

	private static final String URL_GET_CHARGE_STATE = "charge_state";
	private static final String URL_GET_PLUG_STATE = "plug_state";
	private static final String URL_IMPORTED = "imported";


	private final EvcsOpenWb parent;
	private final String baseUrl;
	private Integer energyStartSession = null;
	private Evcs evcs;
	private ElectricityMeter meter;

	protected ReadWorker(EvcsOpenWbImpl parent, Inet4Address ipAddress, int port, int chargePoint)
			throws NoSuchAlgorithmException, KeyManagementException {
		this.parent = parent;
		this.meter = ((ElectricityMeter) this.parent);
		this.evcs = ((Evcs) this.parent);

		this.baseUrl = "https://" + ipAddress.getHostAddress() + ":" + port + "/v1?topic=openWB/internal_chargepoint/" + chargePoint + "/get/";
		this.energyStartSession = null;
		
		this.evcs._setStatus(Status.NOT_READY_FOR_CHARGING);

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
	protected void forever() throws Throwable {
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
		Boolean chargeState = null;
		Boolean plugState = null;
		Integer energyTotal = null;

		final var communicationError = new AtomicBoolean(false);

			try {

				/**
				 * ElectricityMeter channels.
				 *
				 */

				var voltageResponse = this.getResponse(URL_GET_VOLTAGES);
				var voltages = JsonUtils.getAsJsonArray(voltageResponse, "message");
				voltageL1 = JsonUtils.getAsInt(voltages.get(0)) * 1000;
				voltageL2 = JsonUtils.getAsInt(voltages.get(1)) * 1000;
				voltageL3 = JsonUtils.getAsInt(voltages.get(2)) * 1000;
				this.meter._setVoltageL1(voltageL1);
				this.meter._setVoltageL2(voltageL2);
				this.meter._setVoltageL3(voltageL3);

				
				var powerResponse = this.getResponse(URL_GET_POWERS);
				var powers = JsonUtils.getAsJsonArray(powerResponse, "message");
				activePowerL1 = JsonUtils.getAsInt(powers.get(0));
				activePowerL2 = JsonUtils.getAsInt(powers.get(1));
				activePowerL3 = JsonUtils.getAsInt(powers.get(2));
				this.meter._setActivePowerL1(activePowerL1);
				this.meter._setActivePowerL2(activePowerL2);
				this.meter._setActivePowerL3(activePowerL3);

				
				activePower = Math.round(JsonUtils.getAsFloat(this.getResponse(URL_GET_POWER), "message"));
				this.meter._setActivePower(activePower);

				var currentResponse = this.getResponse(URL_GET_CURRENTS);
				var currents = JsonUtils.getAsJsonArray(currentResponse, "message");
				currentL1 = JsonUtils.getAsInt(currents.get(0)) * 1000;
				currentL2 = JsonUtils.getAsInt(currents.get(1)) * 1000;
				currentL3 = JsonUtils.getAsInt(currents.get(2)) * 1000;
				this.meter._setCurrentL1(currentL1);
				this.meter._setCurrentL2(currentL2);
				this.meter._setCurrentL3(currentL3);

				/**
				 * EVCS channels.
				 *
				 */

				this.evcs._setChargingType(ChargingType.AC);

				/*
				 * Read total energy from the box
				 * and calculate session energy
				 *
				 */
				energyTotal = Math.round(JsonUtils.getAsFloat(this.getResponse(URL_IMPORTED), "message"));
				if(this.energyStartSession != null) { 
					this.evcs._setEnergySession((int) Math.max(0, energyTotal - this.energyStartSession));					
				}
				
				/*
				 * There are only two boolean state values
				 * plug state (unplugged and plugged) and charge state (charging and not charging)
				 *
				 */

				plugState = JsonUtils.getAsBoolean(this.getResponse(URL_GET_PLUG_STATE), "message");
				
				chargeState = JsonUtils.getAsBoolean(this.getResponse(URL_GET_CHARGE_STATE), "message");

				if (chargeState) {
					this.evcs._setStatus(Status.CHARGING);
					if (this.evcs.getStatus() == Status.NOT_READY_FOR_CHARGING) { //Session starts if plugged in
						this.energyStartSession = energyTotal;
					}
				} else if (plugState) {
					if (this.evcs.getStatus() == Status.NOT_READY_FOR_CHARGING) { //Session starts if plugged in
						this.energyStartSession = energyTotal;
					}

					this.evcs._setStatus(Status.READY_FOR_CHARGING);
				} else {
					this.evcs._setStatus(Status.NOT_READY_FOR_CHARGING);
				}
								
			} catch (OpenemsNamedException e) {
				communicationError.set(true);
			}

		this.parent._setSlaveCommunicationFailed(communicationError.get());
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
