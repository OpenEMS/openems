package io.openems.edge.io.phoenixcontact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.phoenixcontact.auth.PlcNextTokenManager;
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
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class PlcNextDeviceImpl extends AbstractOpenemsComponent
		implements PlcNextDevice, ElectricityMeter, OpenemsComponent, EventHandler {

	private static final class PlcNextDataProcessor {

		private static final Logger log = LoggerFactory.getLogger(PlcNextDataProcessor.class);

		private final PlcNextDevice device;

		private final List<PlcNextApiCommand> apiCommands;

		public PlcNextDataProcessor(PlcNextDevice device, List<PlcNextApiCommand> apiCommands) {
			this.device = device;
			this.apiCommands = apiCommands;
		}

		public void processData(Event event) {
			if (!device.isEnabled()) {
				log.warn("Module deactivated, skipping event processing of event");
				return;
			}
			List<PlcNextApiCommand> suitableApiCommandsForEvent = apiCommands.stream()
					.filter(item -> item.eventTriggers().contains(event.getTopic())).toList();
			if (suitableApiCommandsForEvent.isEmpty()) {
				log.info("No commands found to be executed");
				return;
			}
			log.info("ECHO: Fetching data " + suitableApiCommandsForEvent.size() + " commands");
			// suitableApiCommandsForEvent.parallelStream().forEach(item -> item.execute());

		}
	}

	private static final Logger log = LoggerFactory.getLogger(PlcNextDeviceImpl.class);

	@Reference
	private PlcNextGdsProvider gdsProvider;

	@Reference
	private PlcNextTokenManager tokenManager;

	private Config config;

	private PlcNextDataProcessor dataProcessor;

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

		if (isInitializationOfDataProcessingRequired()) {
			logInfo(log, "Initializing data processing");
			List<PlcNextApiCommand> apiCommands = buildCommands(config);

			this.dataProcessor = new PlcNextDataProcessor(this, apiCommands);
		}
	}

	private boolean isInitializationOfDataProcessingRequired() {
		return Objects.isNull(dataProcessor);
	}

	private List<PlcNextApiCommand> buildCommands(Config config) {
		logInfo(log, "Building list of API commands");
		List<PlcNextApiCommand> cmds = new ArrayList<PlcNextApiCommand>();

		if (config.dataInstanceNames() != null) {
			for (String instName : config.dataInstanceNames()) {
				cmds.add(new PlcNextReadFromApiResourceCommand(gdsProvider, instName, this.channels()));
			}
		}
		return Collections.unmodifiableList(cmds);
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

	@Override
	public void handleEvent(Event event) {
		logInfo(log, "Handling event '" + event.getTopic() + "'");
		tokenManager.fetchToken();
		dataProcessor.processData(event);
	}
}