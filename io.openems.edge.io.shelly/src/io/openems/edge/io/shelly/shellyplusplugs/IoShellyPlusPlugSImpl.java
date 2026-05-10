package io.openems.edge.io.shelly.shellyplusplugs;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static io.openems.edge.io.shelly.common.Utils.readUpdatesAvailableStatusFromStatusResponse;
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
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.mdns.MDnsDiscovery;
import io.openems.edge.common.type.Phase;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.shelly.common.HttpBridgeShellyService;
import io.openems.edge.io.shelly.common.component.ShellyMeteredSwitch;
import io.openems.edge.io.shelly.common.component.ShellyMeteredSwitchHandler;
import io.openems.edge.io.shelly.common.component.ShellySwitch;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBase;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBaseImpl;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.Plus.PlugS", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_EXECUTE_WRITE, //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class IoShellyPlusPlugSImpl extends IoGen2ShellyBaseImpl
		implements IoShellyPlusPlugs, ShellyMeteredSwitch, ShellySwitch, IoGen2ShellyBase, DigitalOutput,
		SinglePhaseMeter, ElectricityMeter, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(IoShellyPlusPlugSImpl.class);

	private MeterType meterType = null;
	private Phase.SinglePhase phase = null;
	private ShellyMeteredSwitchHandler handler;

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

	public IoShellyPlusPlugSImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SinglePhaseMeter.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoGen2ShellyBase.ChannelId.values(), //
				ShellySwitch.ChannelId.values(), //
				ShellyMeteredSwitch.ChannelId.values(), //
				ShellyMeteredSwitch.ErrorChannelId.values(), //
				IoShellyPlusPlugs.ChannelId.values() //
		);
	}

	@Override
	public String[] getSupportedShellyDeviceTypes() {
		return new String[] { "PlusPlugS" };
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.ip(), config.mdnsName(),
				config.debugMode(), config.validateDevice());

		this.meterType = config.type();
		this.phase = config.phase();
		this.handler = new ShellyMeteredSwitchHandler(this, this.shellyService, 0, config.invert());
	}

	@Override
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
		this.cycleService.subscribeJsonEveryCycle(this.baseUrl + "/rpc/Shelly.GetStatus", this::processHttpResult);
	}

	private void processHttpResult(HttpResponse<JsonElement> result, Throwable error) {
		setValue(this, IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, error != null);

		if (error != null) {
			this.logWarn(this.log, "Failed to fetch status from shelly: " + error.getMessage());
			this.handler.resetSwitchData();
			return;
		}

		try {
			var response = JsonUtils.getAsJsonObject(result.data());

			setValue(this, IoGen2ShellyBase.ChannelId.HAS_UPDATE,
					readUpdatesAvailableStatusFromStatusResponse(response));
			this.handler.processSwitchData(JsonUtils.getAsJsonObject(response, "switch:0"));

		} catch (Exception e) {
			this.logWarn(this.log, "Error while parsing response: " + e.getMessage());
			this.handler.resetSwitchData();
		}
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.handler.getDigitalOutputChannels();
	}

	@Override
	public Phase.SinglePhase getPhase() {
		return this.phase;
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
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