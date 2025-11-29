package io.openems.edge.io.shelly.shellypluspm;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static io.openems.edge.io.shelly.common.Utils.executeWrite;
import static java.lang.Math.round;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.function.IntFunction;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.types.DebugMode;
import io.openems.common.types.MeterType;
import io.openems.common.utils.StringUtils;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.mdns.MDnsDiscovery;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.shelly.shellyplugsbase.IoShellyPlugSBase;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.PlusPM", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_EXECUTE_WRITE, //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class IoShellyPlusPmImpl extends AbstractOpenemsComponent implements IoShellyPlusPm, IoShellyPlugSBase,
		DigitalOutput, SinglePhaseMeter, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private final Logger log = LoggerFactory.getLogger(IoShellyPlusPmImpl.class);

	private MeterType meterType = null;
	private SinglePhase phase = null;
	private boolean invert = false;
	private int channel = 0;
	private String baseUrl;

	private BridgeHttp httpBridge;
	private HttpBridgeCycleService cycleService;
	private AutoCloseable mdnsUnsubscribe;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;
	@Reference
	private MDnsDiscovery mDnsDiscovery;

	public IoShellyPlusPmImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SinglePhaseMeter.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoShellyPlugSBase.ChannelId.values(), //
				IoShellyPlusPm.ChannelId.values() //
		);

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromCurrent(this);
		SinglePhaseMeter.calculateSinglePhaseFromVoltage(this);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = config.type();
		this.phase = config.phase();
		this.invert = config.invert();
		this.channel = config.channel();
		this.httpBridge = this.httpBridgeFactory.get();
		this.httpBridge.setDebugMode(config.debugMode());
		this.cycleService = this.httpBridge.createService(this.httpBridgeCycleServiceDefinition);

		if (!this.isEnabled()) {
			return;
		}

		if (!StringUtils.isNullOrBlank(config.ip())) {
			this.subscribe(config.ip());
			return;
		}

		if (!StringUtils.isNullOrBlank(config.mdnsName())) {
			this.mdnsUnsubscribe = this.mDnsDiscovery.subscribeService("_shelly._tcp.local.", config.mdnsName(),
					event -> {
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
			this.httpBridgeFactory.unget(this.httpBridge);
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
		this.log.info("Subscribing to Shelly at IP {} with channel {}", ip, this.channel);
		this.baseUrl = "http://" + ip;
		this.cycleService.subscribeJsonEveryCycle(this.baseUrl + "/rpc/Shelly.GetStatus", this::processHttpResult);
	}

	private void unsubscribe() {
		this.baseUrl = null;
		if (this.cycleService != null) {
			this.cycleService.removeAllCycleEndpoints();
		}
	}

	@Override
	public io.openems.edge.common.channel.BooleanWriteChannel[] digitalOutputChannels() {
		return new io.openems.edge.common.channel.BooleanWriteChannel[] { //
				this.channel(IoShellyPlugSBase.ChannelId.RELAY) //
		};
	}

	@Override
	public String debugLog() {
		return io.openems.edge.io.shelly.common.Utils.generateDebugLog(this.digitalOutputChannels(),
				this.getActivePowerChannel());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
		case TOPIC_CYCLE_EXECUTE_WRITE -> executeWrite(this.getRelayChannel(), this.baseUrl, this.httpBridge,
				this.channel);
		}
	}

	private void processHttpResult(HttpResponse<JsonElement> result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);

		final IntFunction<Integer> invert = value -> this.invert ? value * -1 : value;

		Boolean relayStatus = null;
		Boolean updatesAvailable = false;
		Boolean restartRequired = false;
		Integer activePower = null;
		Integer current = null;
		Integer voltage = null;

		if (error != null) {
			this.logWarn(this.log, error.getMessage());

		} else {
			try {
				var response = getAsJsonObject(result.data());
				var sysInfo = getAsJsonObject(response, "sys");
				var update = getAsJsonObject(sysInfo, "available_updates");
				updatesAvailable = update != null && !update.entrySet().isEmpty();
				restartRequired = getAsBoolean(sysInfo, "restart_required");

				// Read from the configured channel
				var switchX = getAsJsonObject(response, "switch:" + this.channel);
				activePower = invert.apply(round(getAsFloat(switchX, "apower")));
				current = invert.apply(round(getAsFloat(switchX, "current") * 1000));
				voltage = round(getAsFloat(switchX, "voltage") * 1000);
				relayStatus = getAsBoolean(switchX, "output");

			} catch (Exception e) {
				this.logWarn(this.log, e.getMessage());
			}
		}

		this._setRelay(relayStatus);
		this._setActivePower(activePower);
		this._setCurrent(current);
		this._setVoltage(voltage);
		this.channel(IoShellyPlugSBase.ChannelId.HAS_UPDATE).setNextValue(updatesAvailable);
		this.channel(IoShellyPlusPm.ChannelId.NEEDS_RESTART).setNextValue(restartRequired);
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

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}
