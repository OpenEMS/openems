package io.openems.edge.phoenixcontact.plcnext;

import java.util.List;
import java.util.stream.Stream;

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

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextAuthConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataAccessConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappedValue;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappingException;
import io.openems.edge.phoenixcontact.plcnext.data.PlcNextGdsMeterDataVariableDefinition;
import io.openems.edge.phoenixcontact.plcnext.mapper.PlcNextGdsMeterDataToChannelMapper;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PLCnext.Meter.Device", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class PlcNextDeviceImpl extends AbstractOpenemsComponent
		implements PlcNextDevice, ElectricityMeter, OpenemsComponent, EventHandler {

	private static final Logger log = LoggerFactory.getLogger(PlcNextDeviceImpl.class);

	private static final JsonObject defaultResponse = JsonUtils.buildJsonObject()//
			.add("variables", JsonUtils.buildJsonArray().build()).build();

	@Reference
	private PlcNextGdsDataProvider gdsDataProvider;
	@Reference
	private PlcNextGdsMeterDataToChannelMapper gdsMeterDataToChannelMapper;

	private Config config;
	private PlcNextAuthConfig authConfig;
	private PlcNextGdsDataAccessConfig gdsDataAccessConfig;

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
		this.authConfig = new PlcNextAuthConfig(config.authUrl(), config.username(), config.password());
		this.gdsDataAccessConfig = new PlcNextGdsDataAccessConfig(config.dataUrl(), config.dataInstanceName());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		gdsDataProvider.deactivateSessionMaintenance();

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
		if (!this.isEnabled()) {
			log.warn("Module deactivated, skipping event processing of event");
			return;
		}
		if (EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE == event.getTopic()) {
			processDataOnBeforeProcessImageEvent();
		}
	}

	/**
	 * Triggers fetching and mapping data and pushing to channels
	 */
	void processDataOnBeforeProcessImageEvent() {
		log.info("Reading GDS data from instance '" + gdsDataAccessConfig.dataUrl() + "'");

		List<String> variableIdentifiers = Stream.of(PlcNextGdsMeterDataVariableDefinition.values())//
				.map(PlcNextGdsMeterDataVariableDefinition::getIdentifier).toList();
		JsonObject apiResponseBody = gdsDataProvider.readDataFromRestApi(variableIdentifiers, 
					gdsDataAccessConfig, authConfig)
				.orElse(defaultResponse);

		try {
			List<PlcNextGdsDataMappedValue> mappedValues = gdsMeterDataToChannelMapper.mapAllValuesToChannels(
					apiResponseBody.getAsJsonArray(PlcNextGdsDataProvider.PLC_NEXT_VARIABLES),
					config.dataInstanceName());

			if (!mappedValues.isEmpty()) {
				for (PlcNextGdsDataMappedValue mappedValue : mappedValues) {
					setNextValueToChannel(mappedValue);
				}
			}
		} catch (PlcNextGdsDataMappingException e) {
			log.error("Mapping error!", e);
		}
	}

	/**
	 * Writes value fetched from PLCnext GDS to device channel
	 * 
	 * @param mappedValue represents a value object containing channel ID and value
	 *                    to set to channel
	 * @param device      represents the device holding the channels
	 */
	void setNextValueToChannel(PlcNextGdsDataMappedValue mappedValue) {
		log.info("Providing value '" + mappedValue.getValue() + "' to channel named '" + mappedValue.getChannelId()
				+ "'");
		channel(mappedValue.getChannelId()).setNextValue(mappedValue.getValue());
	}

}
