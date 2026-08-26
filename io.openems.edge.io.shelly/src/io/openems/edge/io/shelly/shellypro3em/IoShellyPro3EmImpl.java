package io.openems.edge.io.shelly.shellypro3em;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.mdns.MDnsDiscovery;
import io.openems.edge.io.shelly.common.HttpBridgeShellyService;
import io.openems.edge.io.shelly.common.component.ShellyEnergyMeter;
import io.openems.edge.io.shelly.common.component.ShellyEnergyMeterHandler;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBase;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBaseImpl;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.Pro3EM", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@EventTopics({ //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class IoShellyPro3EmImpl extends IoGen2ShellyBaseImpl implements IoShellyPro3Em, ShellyEnergyMeter,
		IoGen2ShellyBase, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final Logger log = LoggerFactory.getLogger(IoShellyPro3EmImpl.class);

	private MeterType meterType = null;
	private ShellyEnergyMeterHandler handler;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Reference
	private MDnsDiscovery mDnsDiscovery;
	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;
	@Reference
	private HttpBridgeShellyService.HttpBridgeShellyServiceDefinition httpBridgeShellyServiceDefinition;

	public IoShellyPro3EmImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				IoGen2ShellyBase.ChannelId.values(), //
				ShellyEnergyMeter.ChannelId.values(), //
				ShellyEnergyMeter.ErrorChannelId.values(), //
				IoShellyPro3Em.ChannelId.values() //
		);
	}

	@Override
	public String[] getSupportedShellyDeviceTypes() {
		return new String[] { "Pro3EM" };
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		this.meterType = config.type();

		super.activate(context, config.id(), config.alias(), config.enabled(), config.ip(), config.mdnsName(),
				config.debugMode(), config.validateDevice());

		this.handler = new ShellyEnergyMeterHandler(this, config.invert());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return this.handler.generateDebugLog() //
				+ (this.metricService != null ? ", " + this.metricService : "");
	}

	@Override
	public void handleEvent(Event event) {
		if (this.isEnabled() && this.handler != null) {
			this.handler.handleEvent(event);
		}
	}

	@Override
	protected void subscribeDataCalls() {
		this.cycleService.subscribeJsonEveryCycle(this.baseUrl + "/rpc/EM.GetStatus?id=0", this::processHttpResult);
	}

	private void processHttpResult(HttpResponse<JsonElement> result, HttpError error) {
		setValue(this, IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, error != null);

		if (error != null) {
			this.logWarn(this.log, "Failed to fetch status from shelly: " + error.getMessage());
			this.handler.resetEmData();
			return;
		}

		try {
			var response = JsonUtils.getAsJsonObject(result.data());
			this.handler.processEmData(response);
		} catch (Exception e) {
			this.logWarn(this.log, "Error while parsing response: " + e.getMessage());
			this.handler.resetEmData();
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected BridgeHttpFactory getHttpBridgeFactory() {
		return this.httpBridgeFactory;
	}

	@Override
	protected HttpBridgeCycleServiceDefinition getHttpBridgeCycleServiceDefinition() {
		return this.httpBridgeCycleServiceDefinition;
	}

	@Override
	protected HttpBridgeShellyService.HttpBridgeShellyServiceDefinition getHttpBridgeShellyServiceDefinition() {
		return this.httpBridgeShellyServiceDefinition;
	}

	@Override
	protected MDnsDiscovery getMDnsDiscovery() {
		return this.mDnsDiscovery;
	}
}
