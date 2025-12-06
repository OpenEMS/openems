package io.openems.edge.evcs.openwb;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.common.utils.InetAddressUtils;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.OpenWB", //
		immediate = true, //
		configurationPolicy = REQUIRE)
public class EvcsOpenWbImpl extends AbstractOpenemsComponent
		implements EvcsOpenWb, ElectricityMeter, OpenemsComponent, Evcs, TimedataProvider, ModbusSlave {

	private static final String URL_GET_POWER = "power";
	private static final String URL_GET_POWERS = "powers";
	private static final String URL_GET_VOLTAGES = "voltages";
	private static final String URL_GET_CURRENTS = "currents";
	private static final String URL_GET_CHARGE_STATE = "charge_state";
	private static final String URL_GET_PLUG_STATE = "plug_state";
	private static final String URL_IMPORTED = "imported";

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;

	private BridgeHttp httpBridge;
	private HttpBridgeCycleService cycleService;

	private Config config;
	private String baseUrl;
	private Integer energyStartSession = null;

	public EvcsOpenWbImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				EvcsOpenWb.ChannelId.values() //
		);

		ElectricityMeter.calculateSumActivePowerFromPhases(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	protected void activate(ComponentContext context, Config config)
			throws KeyManagementException, NoSuchAlgorithmException, OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		var ip = InetAddressUtils.parseOrError(config.ip());
		this.baseUrl = "https://" + ip.getHostAddress() + ":" + config.port() + "/v1?topic=openWB/internal_chargepoint/"
				+ config.chargePoint().value + "/get/";

		this._setStatus(Status.NOT_READY_FOR_CHARGING);
		this._setChargingType(ChargingType.AC);

		this.setupSslTrustAll();

		this.httpBridge = this.httpBridgeFactory.get();
		this.cycleService = this.httpBridge.createService(this.httpBridgeCycleServiceDefinition);

		this.subscribeToEndpoints();
	}

	/**
	 * Disables SSL certificate checking for HTTPS connections. OpenWB Series 2 uses
	 * self-signed certificates.
	 */
	private void setupSslTrustAll() throws NoSuchAlgorithmException, KeyManagementException {
		var sslContext = SSLContext.getInstance("TLSv1.2");
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
		sslContext.init(null, trustManager, new SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
	}

	/**
	 * Subscribes to all OpenWB API endpoints.
	 */
	private void subscribeToEndpoints() {
		// Voltages
		this.cycleService.subscribeEveryCycle(//
				this.baseUrl + URL_GET_VOLTAGES, //
				response -> this.handleVoltagesResponse(response.data()), //
				error -> this.handleError());

		// Powers
		this.cycleService.subscribeEveryCycle(//
				this.baseUrl + URL_GET_POWERS, //
				response -> this.handlePowersResponse(response.data()), //
				error -> this.handleError());

		// Total Power
		this.cycleService.subscribeEveryCycle(//
				this.baseUrl + URL_GET_POWER, //
				response -> this.handlePowerResponse(response.data()), //
				error -> this.handleError());

		// Currents
		this.cycleService.subscribeEveryCycle(//
				this.baseUrl + URL_GET_CURRENTS, //
				response -> this.handleCurrentsResponse(response.data()), //
				error -> this.handleError());

		// Imported (Energy)
		this.cycleService.subscribeEveryCycle(//
				this.baseUrl + URL_IMPORTED, //
				response -> this.handleImportedResponse(response.data()), //
				error -> this.handleError());

		// Plug State
		this.cycleService.subscribeEveryCycle(//
				this.baseUrl + URL_GET_PLUG_STATE, //
				response -> this.handlePlugStateResponse(response.data()), //
				error -> this.handleError());

		// Charge State
		this.cycleService.subscribeEveryCycle(//
				this.baseUrl + URL_GET_CHARGE_STATE, //
				response -> this.handleChargeStateResponse(response.data()), //
				error -> this.handleError());
	}

	private void handleVoltagesResponse(String data) {
		try {
			var json = parseToJsonObject(data);
			var voltages = getAsJsonArray(json, "message");
			this._setVoltageL1(getAsInt(voltages.get(0)) * 1000);
			this._setVoltageL2(getAsInt(voltages.get(1)) * 1000);
			this._setVoltageL3(getAsInt(voltages.get(2)) * 1000);
			this._setSlaveCommunicationFailed(false);
		} catch (Exception e) {
			this.resetVoltages();
			this._setSlaveCommunicationFailed(true);
		}
	}

	private void handlePowersResponse(String data) {
		try {
			var json = parseToJsonObject(data);
			var powers = getAsJsonArray(json, "message");
			this._setActivePowerL1(getAsInt(powers.get(0)));
			this._setActivePowerL2(getAsInt(powers.get(1)));
			this._setActivePowerL3(getAsInt(powers.get(2)));
			this._setSlaveCommunicationFailed(false);
		} catch (Exception e) {
			this.resetPowers();
			this._setSlaveCommunicationFailed(true);
		}
	}

	private void handlePowerResponse(String data) {
		try {
			var json = parseToJsonObject(data);
			this._setActivePower(Math.round(getAsFloat(json, "message")));
			this._setSlaveCommunicationFailed(false);
		} catch (Exception e) {
			this._setActivePower(null);
			this._setSlaveCommunicationFailed(true);
		}
	}

	private void handleCurrentsResponse(String data) {
		try {
			var json = parseToJsonObject(data);
			var currents = getAsJsonArray(json, "message");
			this._setCurrentL1(getAsInt(currents.get(0)) * 1000);
			this._setCurrentL2(getAsInt(currents.get(1)) * 1000);
			this._setCurrentL3(getAsInt(currents.get(2)) * 1000);
			this._setSlaveCommunicationFailed(false);
		} catch (Exception e) {
			this.resetCurrents();
			this._setSlaveCommunicationFailed(true);
		}
	}

	private void handleImportedResponse(String data) {
		try {
			var json = parseToJsonObject(data);
			var energyTotal = Math.round(getAsFloat(json, "message"));

			// Set energy channels directly from OpenWB readings
			// EVCS only consumes energy, so production is always 0
			this._setActiveProductionEnergy(0L);
			this._setActiveConsumptionEnergy(energyTotal);

			// Calculate session energy
			if (this.energyStartSession != null) {
				var energySession = (int) Math.max(0, energyTotal - this.energyStartSession);
				this._setEnergySession(energySession);
			}
			this._setSlaveCommunicationFailed(false);
		} catch (Exception e) {
			this._setActiveProductionEnergy(null);
			this._setActiveConsumptionEnergy(null);
			this._setEnergySession(null);
			this._setSlaveCommunicationFailed(true);
		}
	}

	// Keep track of plug/charge states to determine status
	private Boolean lastPlugState = null;
	private Boolean lastChargeState = null;

	private void handlePlugStateResponse(String data) {
		try {
			var json = parseToJsonObject(data);
			this.lastPlugState = getAsBoolean(json, "message");
			this.updateStatus();
			this._setSlaveCommunicationFailed(false);
		} catch (Exception e) {
			this.lastPlugState = null;
			this._setStatus(null);
			this._setSlaveCommunicationFailed(true);
		}
	}

	private void handleChargeStateResponse(String data) {
		try {
			var json = parseToJsonObject(data);
			this.lastChargeState = getAsBoolean(json, "message");
			this.updateStatus();
			this._setSlaveCommunicationFailed(false);
		} catch (Exception e) {
			this.lastChargeState = null;
			this._setStatus(null);
			this._setSlaveCommunicationFailed(true);
		}
	}

	/**
	 * Updates the EVCS status based on plug and charge states.
	 */
	private void updateStatus() {
		if (this.lastPlugState == null || this.lastChargeState == null) {
			return;
		}

		Status previousStatus = this.getStatus();
		Status newStatus;

		if (this.lastChargeState) {
			newStatus = Status.CHARGING;
			if (previousStatus == Status.NOT_READY_FOR_CHARGING) {
				// Session starts when plugged in and charging
				this.startNewSession();
			}
		} else if (this.lastPlugState) {
			newStatus = Status.READY_FOR_CHARGING;
			if (previousStatus == Status.NOT_READY_FOR_CHARGING) {
				// Session starts when plugged in
				this.startNewSession();
			}
		} else {
			newStatus = Status.NOT_READY_FOR_CHARGING;
		}

		this._setStatus(newStatus);
	}

	/**
	 * Starts a new charging session by recording the current energy total.
	 */
	private void startNewSession() {
		// Get current energy total from the imported response
		// This will be set when the next imported response comes in
		try {
			var response = this.httpBridge.get(this.baseUrl + URL_IMPORTED).get();
			var json = parseToJsonObject(response.data());
			this.energyStartSession = Math.round(getAsFloat(json, "message"));
		} catch (Exception e) {
			// Will be set on next cycle
		}
	}

	private void handleError() {
		this._setSlaveCommunicationFailed(true);
		this.resetAllChannels();
	}

	private void resetVoltages() {
		this._setVoltageL1(null);
		this._setVoltageL2(null);
		this._setVoltageL3(null);
	}

	private void resetPowers() {
		this._setActivePowerL1(null);
		this._setActivePowerL2(null);
		this._setActivePowerL3(null);
	}

	private void resetCurrents() {
		this._setCurrentL1(null);
		this._setCurrentL2(null);
		this._setCurrentL3(null);
	}

	private void resetAllChannels() {
		this.resetVoltages();
		this.resetPowers();
		this.resetCurrents();
		this._setActivePower(null);
		this._setActiveProductionEnergy(null);
		this._setActiveConsumptionEnergy(null);
		this._setStatus(null);
		this._setEnergySession(null);
	}

	@Deactivate
	protected void deactivate() {
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.CONSUMPTION_METERED;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(EvcsOpenWb.class, accessMode, 100) //
						.build());
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}
}
