package io.openems.edge.phoenixcontact.plcnext;

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
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.auth.PlcNextTokenManagerConfig;
import io.openems.edge.phoenixcontact.plcnext.gds.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.gds.PlcNextGdsDataProviderConfig;

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

	private static final Logger log = LoggerFactory.getLogger(PlcNextDeviceImpl.class);

	@Reference
	private PlcNextGdsDataProvider gdsProvider;

	@Reference
	private PlcNextTokenManager tokenManager;

	private Config config;
	private PlcNextTokenManagerConfig tokenManagerConfig;
	private PlcNextGdsDataProviderConfig gdsDataProviderConfig;

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
		this.tokenManagerConfig = new PlcNextTokenManagerConfig(config.authUrl(), config.username(), config.password());
		this.gdsDataProviderConfig = new PlcNextGdsDataProviderConfig(config.dataUrl(), config.dataInstanceName(),
				this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
//		gdsProvider.deactivateSessionMaintenance();

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
		processData(event);
	}

	void processData(Event event) {
		if (!this.isEnabled()) {
			log.warn("Module deactivated, skipping event processing of event");
			return;
		}
		if (EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE == event.getTopic()) {
			log.info("Reading GDS data from instance '" + gdsDataProviderConfig.dataUrl() + "'");
			tokenManager.fetchToken(tokenManagerConfig);
			gdsProvider.readFromApiToChannels(gdsDataProviderConfig);
		}
	}

}
