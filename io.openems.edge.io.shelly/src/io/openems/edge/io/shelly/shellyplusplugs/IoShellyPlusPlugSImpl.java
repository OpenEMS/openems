package io.openems.edge.io.shelly.shellyplusplugs;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
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

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.mdns.MDnsDiscovery;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.shelly.shellyplugsbase.IoShellyPlugSBase;
import io.openems.edge.io.shelly.shellyplugsbase.IoShellyPlugSBaseImpl;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

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
public class IoShellyPlusPlugSImpl extends IoShellyPlugSBaseImpl implements IoShellyPlusPlugs, DigitalOutput,
		SinglePhaseMeter, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;
	@Reference
	private MDnsDiscovery mDnsDiscovery;

	public IoShellyPlusPlugSImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SinglePhaseMeter.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoShellyPlugSBase.ChannelId.values(), //
				IoShellyPlusPlugs.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.type(), config.phase(),
				config.invert(), config.ip(), config.mdnsName(), config.debugMode(),
				config.validateDevice() ? new ShellyValidation("PlusPlugS") : null);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
	}

	@Override
	protected BridgeHttpFactory getBridgeHttpFactory() {
		return this.httpBridgeFactory;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	protected HttpBridgeCycleServiceDefinition getHttpBridgeCycleServiceDefinition() {
		return this.httpBridgeCycleServiceDefinition;
	}

	@Override
	protected MDnsDiscovery getMDnsDiscovery() {
		return this.mDnsDiscovery;
	}

}