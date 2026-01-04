package io.openems.edge.io.shelly.shellyplugsbase;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static io.openems.edge.io.shelly.common.Utils.executeWrite;
import static io.openems.edge.io.shelly.common.Utils.generateDebugLog;
import static java.lang.Math.round;

import java.util.function.IntFunction;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.types.DebugMode;
import io.openems.common.types.MeterType;
import io.openems.common.utils.StringUtils;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.mdns.MDnsDiscovery;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

/**
 * Base class for shelly plugs gen2 and gen3. Implements meter values and relay.
 */
public abstract class IoShellyPlugSBaseImpl extends AbstractOpenemsComponent implements IoShellyPlugSBase,
		DigitalOutput, SinglePhaseMeter, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	public record ShellyValidation(String shellyAppName) {

	}

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private final Logger log = LoggerFactory.getLogger(IoShellyPlugSBaseImpl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;

	private MeterType meterType = null;
	private SinglePhase phase = null;
	private boolean invert = false;
	private String baseUrl;
	private ShellyValidation shellyValidation;

	private BridgeHttp httpBridge;
	private HttpBridgeCycleService cycleService;
	private HttpBridgeTimeService timeService;

	private AutoCloseable mdnsUnsubscribe;

	protected IoShellyPlugSBaseImpl(//
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, //
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds //
	) {
		super(//
				firstInitialChannelIds, //
				furtherInitialChannelIds //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(IoShellyPlugSBase.ChannelId.RELAY) //
		};

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromCurrent(this);
		SinglePhaseMeter.calculateSinglePhaseFromVoltage(this);
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled, MeterType type,
			SinglePhase phase, boolean invert, String ip, String mdnsName, DebugMode debugMode,
			ShellyValidation shellyValidation) {
		super.activate(context, id, alias, enabled);
		this.meterType = type;
		this.phase = phase;
		this.invert = invert;
		this.httpBridge = this.getBridgeHttpFactory().get();
		this.httpBridge.setDebugMode(debugMode);
		this.cycleService = this.httpBridge.createService(this.getHttpBridgeCycleServiceDefinition());
		this.timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
		this.shellyValidation = shellyValidation;

		if (!this.isEnabled()) {
			return;
		}

		if (!StringUtils.isNullOrBlank(ip)) {
			this.subscribe(ip);
			return;
		}

		if (!StringUtils.isNullOrBlank(mdnsName)) {
			this.mdnsUnsubscribe = this.getMDnsDiscovery().subscribeService("_shelly._tcp.local.", mdnsName, event -> {
				switch (event) {
				case MDnsDiscovery.MDnsEvent.ServiceAdded serviceAdded -> {
					// Do nothing, wait for resolved event
				}
				case MDnsDiscovery.MDnsEvent.ServiceResolved serviceResolved -> {
					if (serviceResolved.addresses().isEmpty()) {
						return;
					}
					final var dynamicIp = serviceResolved.addresses().getFirst();
					this.unsubscribe();
					this.subscribe(dynamicIp.getHostAddress());
				}
				case MDnsDiscovery.MDnsEvent.ServiceRemoved serviceRemoved -> {
					this.unsubscribe();
				}
				}
			});
			return;
		}

		this.logWarn(this.log, "No valid IP or MDNS Name configured.");
		this._setSlaveCommunicationFailed(true);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.httpBridge != null) {
			this.getBridgeHttpFactory().unget(this.httpBridge);
			this.httpBridge = null;
		}
		if (this.mdnsUnsubscribe != null) {
			try {
				this.mdnsUnsubscribe.close();
			} catch (Exception e) {
				this.logWarn(this.log, "Error during MDNS unsubscribe: " + e.getMessage());
			}
			this.mdnsUnsubscribe = null;
		}
		super.deactivate();
	}

	private void subscribe(String ip) {
		this.log.info("Subscribing to Shelly at IP {}", ip);
		this.baseUrl = "http://" + ip;

		final var validation = this.shellyValidation;
		if (validation != null) {
			this.timeService.subscribeJsonTime(new ValidateTimeEndpointDelayTimeProvider(),
					BridgeHttp.create(this.baseUrl + "/rpc/Shelly.GetDeviceInfo").build(), (result, httpError) -> {
						if (httpError != null) {
							this._setWrongDeviceType(false);
							return;
						}
						final var deviceInfo = DeviceInfo.serializer().deserialize(result.data());
						this._setWrongDeviceType(!deviceInfo.app().equals(validation.shellyAppName()));
					});
		} else {
			this._setWrongDeviceType(false);
		}
		this.cycleService.subscribeJsonEveryCycle(this.baseUrl + "/rpc/Shelly.GetStatus", this::processHttpResult);
	}

	private void unsubscribe() {
		this.baseUrl = null;
		this.cycleService.removeAllCycleEndpoints();
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this.digitalOutputChannels, this.getActivePowerChannel());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
		case TOPIC_CYCLE_EXECUTE_WRITE -> executeWrite(this.getRelayChannel(), this.baseUrl, this.httpBridge, 0);
		}
	}

	private void processHttpResult(HttpResponse<JsonElement> result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);

		if (this.getWrongDeviceType().orElse(false)) {
			// do not apply values if device-type is wrong
			this.resetValues();
			return;
		}

		if (error != null) {
			this.logWarn(this.log, error.getMessage());
			this.resetValues();
			return;
		}

		final IntFunction<Integer> invert = value -> this.invert ? value * -1 : value;

		Boolean relayStatus = null;
		boolean updatesAvailable = false;
		Integer activePower = null;
		Integer current = null;
		Integer voltage = null;
		try {
			var response = getAsJsonObject(result.data());
			var sysInfo = getAsJsonObject(response, "sys");
			var update = getAsJsonObject(sysInfo, "available_updates");
			updatesAvailable = !update.entrySet().isEmpty();

			var relays = getAsJsonObject(response, "switch:0");
			activePower = invert.apply(round(getAsFloat(relays, "apower")));
			current = invert.apply(round(getAsFloat(relays, "current") * 1000));
			voltage = round(getAsFloat(relays, "voltage") * 1000);
			relayStatus = getAsBoolean(relays, "output");

		} catch (Exception e) {
			this.logWarn(this.log, e.getMessage());
		}

		this.updateValues(relayStatus, activePower, current, voltage, updatesAvailable);
	}

	private void resetValues() {
		this.updateValues(null, null, null, null, false);
	}

	private void updateValues(Boolean relayStatus, Integer activePower, Integer current, Integer voltage,
			boolean updatesAvailable) {
		this._setRelay(relayStatus);
		this._setActivePower(activePower);
		this._setCurrent(current);
		this._setVoltage(voltage);
		this.channel(IoShellyPlugSBase.ChannelId.HAS_UPDATE).setNextValue(updatesAvailable);
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower >= 0) {
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(-activePower);
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	public SinglePhase getPhase() {
		return this.phase;
	}

	protected abstract BridgeHttpFactory getBridgeHttpFactory();

	protected abstract HttpBridgeCycleServiceDefinition getHttpBridgeCycleServiceDefinition();

	protected abstract MDnsDiscovery getMDnsDiscovery();

}