package io.openems.edge.io.phoenixcontact;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.cycle.CycleSubscriber;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.phoenixcontact.gds.PlcNextApiCommand;
import io.openems.edge.io.phoenixcontact.gds.PlcNextGdsProvider;
import io.openems.edge.io.phoenixcontact.gds.PlcNextReadFromApiResourceCommand;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PxC.PLCnext.Device", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class PlcNextDeviceImpl extends AbstractOpenemsComponent
		implements PlcNextDevice, ElectricityMeter, OpenemsComponent {

	private static final Logger log = LoggerFactory.getLogger(PlcNextDeviceImpl.class);

	@Reference
	private CycleSubscriber cycleSubscriber;

	@Reference
	private PlcNextGdsProvider gdsProvider;
	
	private Config config;
	private List<PlcNextApiCommand> apiCommands;

	public PlcNextDeviceImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				PlcNextDevice.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.config = config;
		this.apiCommands = buildCommandsIfNecessary(config, apiCommands);
		this.cycleSubscriber.subscribe((event) -> {
			if (!this.isEnabled()) {
				log.warn("Module deactivated, skipping event processing of event");
				return;
			}
			List<PlcNextApiCommand> apiCmdForEvent = apiCommands.stream()
					.filter(item -> item.eventTriggers().contains(event.getTopic()))
					.toList();
			if (apiCmdForEvent.isEmpty()) {
				log.info("No commands found to be executed");
				return;
			}
			apiCmdForEvent.parallelStream().forEach(item -> item.execute());
		});
	}
	
	private List<PlcNextApiCommand> buildCommandsIfNecessary(Config config, List<PlcNextApiCommand> apiCommands) {
		List<PlcNextApiCommand> newApiCommands = apiCommands;
		
		if (Objects.isNull(apiCommands) || apiCommands.isEmpty()) {
			newApiCommands = List.of(new PlcNextReadFromApiResourceCommand(gdsProvider, config.dataInstanceName()));
		}
		return newApiCommands;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}
}
